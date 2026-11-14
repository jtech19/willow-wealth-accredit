package com.willow.wealth.accredit.fixtures;

import java.time.Instant;
import java.util.UUID;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.domain.AccreditationType;
import com.willow.wealth.accredit.event.AccreditationEvent;

/**
 * Builds {@link AccreditationEvent}s with defaults.
 * Example: {@code aStatusChangedEvent().from(PENDING).to(CONFIRMED).build()}.
 */
public final class AccreditationEventTestBuilder {

	private UUID eventId = UUID.randomUUID();

	private UUID accreditationId = TestData.ACCREDITATION_ID;

	private String userId = TestData.USER_ID;

	private AccreditationType accreditationType = AccreditationType.BY_INCOME;

	private AccreditationStatus previousStatus;

	private AccreditationStatus status;

	private Instant occurredAt = TestData.NOW;

	private AccreditationEventTestBuilder() {
	}

	/** Creation event: no previous status, PENDING. */
	public static AccreditationEventTestBuilder aCreatedEvent() {
		AccreditationEventTestBuilder builder = new AccreditationEventTestBuilder();
		builder.status = AccreditationStatus.PENDING;
		return builder;
	}

	/** Status change event: PENDING to CONFIRMED unless overridden. */
	public static AccreditationEventTestBuilder aStatusChangedEvent() {
		AccreditationEventTestBuilder builder = new AccreditationEventTestBuilder();
		builder.previousStatus = AccreditationStatus.PENDING;
		builder.status = AccreditationStatus.CONFIRMED;
		return builder;
	}

	public AccreditationEventTestBuilder withEventId(UUID eventId) {
		this.eventId = eventId;
		return this;
	}

	public AccreditationEventTestBuilder forAccreditation(UUID accreditationId) {
		this.accreditationId = accreditationId;
		return this;
	}

	public AccreditationEventTestBuilder forUser(String userId) {
		this.userId = userId;
		return this;
	}

	public AccreditationEventTestBuilder ofType(AccreditationType accreditationType) {
		this.accreditationType = accreditationType;
		return this;
	}

	public AccreditationEventTestBuilder from(AccreditationStatus previousStatus) {
		this.previousStatus = previousStatus;
		return this;
	}

	public AccreditationEventTestBuilder to(AccreditationStatus status) {
		this.status = status;
		return this;
	}

	public AccreditationEventTestBuilder occurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
		return this;
	}

	public AccreditationEvent build() {
		return new AccreditationEvent(eventId, accreditationId, userId, accreditationType, previousStatus,
				status, occurredAt);
	}

}
