package com.willow.wealth.accredit.query.model;

import java.time.Instant;
import java.util.UUID;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.domain.AccreditationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read model (query side): the current state of one accreditation.
 * Only written by the Kafka projector; everything else reads it.
 */
@Getter
@Entity
@Table(name = "accreditation_status")
public class Accreditation {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "accreditation_id", length = 36, nullable = false, updatable = false)
	private UUID accreditationId;

	@Column(name = "user_id", length = 64, nullable = false, updatable = false)
	private String userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "accreditation_type", length = 20, nullable = false, updatable = false)
	private AccreditationType accreditationType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private AccreditationStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Accreditation() {
		// for JPA
	}

	private Accreditation(UUID accreditationId, String userId, AccreditationType accreditationType,
			AccreditationStatus status, Instant createdAt) {
		this.accreditationId = accreditationId;
		this.userId = userId;
		this.accreditationType = accreditationType;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	/** A newly submitted accreditation, always starting as PENDING. */
	public static Accreditation pending(UUID accreditationId, String userId, AccreditationType accreditationType,
			Instant createdAt) {
		return new Accreditation(accreditationId, userId, accreditationType, AccreditationStatus.PENDING, createdAt);
	}

	/**
	 * Applies a status change that already happened (an event). Transition rules
	 * are enforced on the command side before the event is published.
	 */
	public void applyStatus(AccreditationStatus newStatus, Instant changedAt) {
		this.status = newStatus;
		this.updatedAt = changedAt;
	}

}
