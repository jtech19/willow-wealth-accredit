# accredit-service

Tracks users' accreditation status for WillowWealth. Admins create and finalise accreditations; the client app reads the current status.

## Prerequisites

- Java 21
- Docker with Compose v2 (must be running for the tests too, they use Testcontainers)
- Required ports (must not be in use by other applications):
  - `9999`: service
  - `3306`: MySQL
  - `9092`: Kafka
  - `8080`: Kafka UI (optional)

No Maven install needed, the project ships with the Maven wrapper (`./mvnw`, or `mvnw.cmd` on Windows).

## Build

Without tests:

```bash
./mvnw clean package -DskipTests
```

With tests:

```bash
./mvnw clean verify
```

## Run

### 1. Start the infrastructure (MySQL + Kafka)

- Start:
  ```bash
  docker compose up -d --wait
  ```
- Stop:
  ```bash
  docker compose down
  ```
- Data is kept in `./.data/` (git-ignored) across restarts. Delete that folder to start fresh.

### 2. Kafka

- Starts with the command above, available on `localhost:9092`.
- To also start the Kafka UI for browsing events:
  ```bash
  docker compose --profile tools up -d --wait
  ```
- Open http://localhost:8080
- Stop everything, UI included:
  ```bash
  docker compose --profile tools down
  ```

### 3. Start the service

- With Maven:
  ```bash
  ./mvnw spring-boot:run
  ```
- Or from the built jar:
  ```bash
  java -jar target/accredit-service-0.0.1-SNAPSHOT.jar
  ```
- Runs on http://localhost:9999. Check it's up:
  ```bash
  curl http://localhost:9999/actuator/health
  ```
- Connection settings can be overridden with environment variables:
  - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
  - `KAFKA_BOOTSTRAP_SERVERS`
  - `ENV` (default: `dev`)
  - `EXPIRY_CRON` (default: hourly, `0 0 * * * *`)

---

## Assumptions

- **DB-level enum checks:** the `CHECK` constraints on `accreditation_type` and `status` are an extra safety net, not a requirement. The read model has only one writer (the Kafka projector), which already uses Java enums. Downsides:
  - Adding a new enum value needs a Flyway migration as well as the code change.
  - If the two get out of sync, inserts fail at runtime.
- **Command side decides, projector records:**
  - The admin endpoints validate the request and status rules before publishing an event. Nothing is published if validation fails.
  - The projector trusts published events and applies them to MySQL without re-checking the rules; othwrwise a valid event might be rejected due to timing.
- **Document is validated, not stored:** the `document` in the create request is validated but not stored or put on the event. The spec only requires events to capture the state change and timestamp. In PROD the file would go to object storage (e.g. S3), with the event carrying a reference to it.
- **Document checks:** `name`, `mime_type` and `content` are required,
  - `content` must be valid base64 of at most 10 MB
    - The content isn't checked against the mime type, since the spec example doesn't decodes to a real PDF.
  - `mime_type` must be:
    - `application/pdf`
    - `image/png`
    - `image/jpeg`
- **Timestamps in UTC:** Hibernate stores and reads timestamps in UTC, so the 30-day expiry doesn't depend on the host's timezone.
- **Unknown user returns an empty map:** there's no user registry, so `GET /user/{userId}/accreditation` returns `200` with empty `accreditation_statuses` rather than `404`.
- **GET is eventually consistent:** it reads the projected read model, so a change made by an admin shows up once the projector has applied the event (normally milliseconds).
- **Finalize returns `200 OK`:** with the same `accreditation_id` body as create. 
  - `PENDING` isn't a valid `outcome` (`400`), an unknown id is `404`, and a transition the rules don't allow (e.g. from `FAILED`) is `409`.
- **Error handling uses Spring's ProblemDetail (RFC 9457):**
  - `400` names invalid or unknown fields
  - Kafka down is `503` (safe to retry)
  - Unexpected is `500` (details only in logs)
  - Extends `ResponseEntityExceptionHandler` so Spring's own errors (`405`, `415`...) keep their status
- **Expiry runs hourly:** CONFIRMED accreditations last updated more than 30 days ago are expired through the same command path as an admin PUT, so each gets an EXPIRED event. A failure is logged; the accreditation is still CONFIRMED, so the next run picks it up again.
- **Events ignore unknown fields:** unlike the REST API, the Kafka consumer accepts events with extra fields, so a new event field doesn't break older consumers.

## Limitations

- **Credentials in git:** the DB credentials in `docker-compose.yml` and `application.yml` are local dev defaults only. In PROD they wouldn't be committed to git. They'd be stored in a secrets manager such as HashiCorp Vault or AWS Secrets Manager, and injected at deploy time through the Helm chart as part of the CD pipeline (e.g. ArgoCD or Jenkins).
- **No high-level design (HLD):** for a real project an HLD might be written first, weighing up the options for:
  - How data flows between the components
  - The tech stack
  - Expected load and scaling
  - Caching (in-memory or distributed)
  - Metrics and alerting
  - The exact flavour of CQRS (e.g. shared vs separate write store, event sourcing or not)
- **No dead-letter topic:** an event the projector can't apply (malformed, or still failing after 2 retries) is logged and skipped so it can't block the partition. In PROD it would go to a dead-letter topic, with an alert, for investigation and replay.
- **Synchronous Kafka publishing:** admin requests block until Kafka acknowledges the event. This was the quickest reliable option for this exercise, since the admin only gets a success response once the event is safely stored.
  - Quick fix: enable Java 21 virtual threads (`spring.threads.virtual.enabled: true`)
  - More scalable fix: the transactional outbox pattern. The event is saved to a DB table in the same transaction, and a relay publishes it to Kafka asynchronously.
- **Duplicate PENDING race condition:** the "one PENDING per user" check reads the read model, which lags behind until the projector applies the event. Two creates for the same user a few ms apart could both pass. Finalize has the same gap: two outcomes for one accreditation (e.g. CONFIRMED and FAILED) could both be accepted, and a PUT sent right after the POST may get a `404`. The proper fix is a command-side write model:
  - A `user_accreditations` aggregate with `@Version`: stops two requests from both passing the check.
  - A transactional outbox: the event is saved in the same transaction as the aggregate, so the two can't drift apart.
  - Kafka keyed by `userId`: keeps each user's events in order.
- **Expiry job runs on every instance:** with several instances, each would publish the same EXPIRED events. The projector ignores the duplicates, but the audit log gets them. In PROD a lock such as ShedLock would make it run once per schedule.
- **Expiry loads all matches at once:** fine for this volume; in PROD it would page through them (e.g. 500 at a time) to keep memory and run time bounded.
