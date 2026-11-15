#!/usr/bin/env bash
# Starts MySQL + Kafka, builds the service and runs it on http://localhost:9999.
# Usage: ./run.sh        (Ctrl+C stops the service; `docker compose down` stops MySQL + Kafka)
set -euo pipefail

cd "$(dirname "$0")"

command -v docker >/dev/null || { echo "Docker is required" >&2; exit 1; }
command -v java >/dev/null || { echo "Java 21 is required" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "Docker is not running" >&2; exit 1; }

echo "==> Starting MySQL and Kafka"
docker compose up -d --wait

echo "==> Building the service (tests skipped; run ./mvnw verify for those)"
./mvnw -q clean package -DskipTests

jar=$(ls target/accredit-service-*.jar | grep -v '\.original$' | head -n 1)

echo "==> Starting the service on http://localhost:9999"
echo "    POST /user/accreditation"
echo "    PUT  /user/accreditation/{accreditationId}"
echo "    GET  /user/{userId}/accreditation"
exec java -jar "$jar"
