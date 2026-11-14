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

---

## Assumptions

- **DB-level enum checks:** the `CHECK` constraints on `accreditation_type` and `status` are an extra safety net, not a requirement. The read model has only one writer (the Kafka projector), which already uses Java enums. Downsides:
  - Adding a new enum value needs a Flyway migration as well as the code change.
  - If the two get out of sync, inserts fail at runtime.

## Limitations

- **Credentials in git:** the DB credentials in `docker-compose.yml` and `application.yml` are local dev defaults only. In production they wouldn't be committed to git. They'd be stored in a secrets manager such as HashiCorp Vault or AWS Secrets Manager, and injected at deploy time through the Helm chart as part of the CD pipeline (e.g. ArgoCD or Jenkins).
