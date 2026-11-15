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

### Quick start (Linux / macOS / Git Bash)

- Starts MySQL and Kafka, builds the jar and runs the service on http://localhost:9999:
  ```bash
  ./run.sh
  ```
- `Ctrl+C` stops the service, `docker compose down` stops MySQL and Kafka.
- Or run each step yourself, as below.

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

### 4. Try it

- Create an accreditation (returns `201` with the `accreditation_id`):
  ```bash
  curl -i -X POST http://localhost:9999/user/accreditation \
    -H 'Content-Type: application/json' \
    -d '{
          "user_id": "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V",
          "accreditation_type": "BY_INCOME",
          "document": {
            "name": "2018.pdf",
            "mime_type": "application/pdf",
            "content": "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg=="
          }
        }'
  ```
- Finalize it (use the `accreditation_id` from above):
  ```bash
  curl -i -X PUT http://localhost:9999/user/accreditation/<accreditation_id> \
    -H 'Content-Type: application/json' \
    -d '{ "outcome": "CONFIRMED" }'
  ```
- Get the user's statuses:
  ```bash
  curl -i http://localhost:9999/user/g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V/accreditation
  ```
- Running the create twice returns `409`, since the user already has a PENDING request.

---

## Questions

### a) Architecture overview

```
POST / PUT / expiry job ──> check MySQL ──> Kafka ──> projector ──> MySQL <── GET
      (command side)                     (events)  (only writer)        (query side)
```

- **CQRS:** separate write and read paths
  - Command side checks the rules, then publishes an event (never writes the DB)
  - Query side: `GET` reads MySQL only
- **Event-driven:** every status change is a Kafka event, which doubles as the audit history
- **Single-writer, idempotent projector:** the only writer of MySQL, so replays and duplicates are safe
- **State machine:** allowed transitions live in `AccreditationStatus`

### b) Concurrent admin updates

- Give the command side its own write model instead of checking the lagging read model:
  - **Aggregate per user with `@Version`:** only one of two concurrent updates can save; the other re-checks the latest state and gets `409`
  - **Transactional outbox:** state change and event saved in one transaction, then relayed to Kafka (e.g. Debezium)
  - **Kafka keyed by `userId`:** a user's events stay in order
  - **Optional `If-Match` / ETag on `PUT`:** an admin can't overwrite a change they haven't seen (`412`)

### c) Scaling client traffic

1. **Measure:** load test; watch latency, errors and consumer lag
2. **Split deployables:** query side (`GET`) as its own service, scaled out; admin side stays small
3. **Scale reads:** MySQL read replicas and a cache (e.g. Redis), evicted by the projector on new events
4. **Scale the projector:** more partitions and consumers if lag grows
5. **Protect:** API gateway rate limiting, autoscaling, separate connection pools per side

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
  - More scalable fix: the transactional outbox from [b)](#b-concurrent-admin-updates)
- **Concurrent admin updates:** checks read the lagging read model, so two requests a few ms apart can both pass (e.g. two PENDING for one user). Fix outlined in [b)](#b-concurrent-admin-updates).
- **Expiry job runs on every instance:** with several instances, each would publish the same EXPIRED events. The projector ignores the duplicates, but the audit log gets them. In PROD a lock such as ShedLock would make it run once per schedule.
- **Expiry loads all matches at once:** fine for this volume; in PROD it would page through them (e.g. 500 at a time) to keep memory and run time bounded.

## AI usage

For this code test I made use of Claude AI. I used it to:
  - clarify requirements from the interview spec sheet
  - verify high-level plan of how to split the work in feature branches where each deliverable is clear & understandable
  - generation of boilerplate code (like DTOs)
  - ensure at least 80-85% coverage of unit tests & IT tests
  - draft the README based on assumptions I made & limitations I had due to time constraints & nature of this exercise being a code test
One of the main points is that I drove the design decisions, after researching & reviewing the options & took decisions balancing time, scope of this service & readiness for future proofing 
It was also useful for weighing trade-offs, such as the CQRS consistency gaps and error-handling options, before committing to an approach.
