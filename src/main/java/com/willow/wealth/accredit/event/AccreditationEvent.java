package com.willow.wealth.accredit.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.domain.AccreditationType;

/**
 * One state change of an accreditation, published to Kafka for auditing and
 * consumed by the projector to build the read model.
 * <p>
 * Every event is self-contained (user, type and new status are always present),
 * so the projector never needs to look anything up to apply it.
 *
 * @param eventId unique per event, lets consumers skip duplicates
 * @param previousStatus {@code null} when the accreditation was just created
 * @param occurredAt when the change happened
 */
public record AccreditationEvent(
		UUID eventId,
		UUID accreditationId,
		String userId,
		AccreditationType accreditationType,
		AccreditationStatus previousStatus,
		AccreditationStatus status,
		Instant occurredAt) {

	public static AccreditationEvent created(UUID accreditationId, String userId, AccreditationType accreditationType,
			Instant occurredAt) {

		return new AccreditationEvent(UUID.randomUUID(), accreditationId, userId, accreditationType, null,
				AccreditationStatus.PENDING, occurredAt);
	}

	public static AccreditationEvent statusChanged(UUID accreditationId, String userId,
			AccreditationType accreditationType, AccreditationStatus previousStatus, AccreditationStatus status,
			Instant occurredAt) {

		return new AccreditationEvent(UUID.randomUUID(), accreditationId, userId, accreditationType, previousStatus,
				status, occurredAt);
	}

	/** {@code true} for the event that created the accreditation (no previous status). */
	@JsonIgnore
	public boolean isCreation() {
		return previousStatus == null;
	}

}
