-- Read model (CQRS query side): current state of each accreditation,
-- projected from the accreditation events published to Kafka.
CREATE TABLE accreditation_status
(
    accreditation_id   CHAR(36)    NOT NULL,
    user_id            VARCHAR(64) NOT NULL,
    accreditation_type VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,

    CONSTRAINT pk_accreditation_status PRIMARY KEY (accreditation_id),
    CONSTRAINT ck_accreditation_status_type
        CHECK (accreditation_type IN ('BY_INCOME', 'BY_NET_WORTH')),
    CONSTRAINT ck_accreditation_status_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'FAILED')),

    -- required to retrieve a user's accreditation and the "no second PENDING" check
    INDEX idx_accreditation_status_user_id_status (user_id, status),
    -- required for expiry job: CONFIRMED rows last updated more than 30 days ago
    INDEX idx_accreditation_status_status_updated_at (status, updated_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
