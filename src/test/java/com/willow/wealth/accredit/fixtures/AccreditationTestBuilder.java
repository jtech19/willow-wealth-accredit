package com.willow.wealth.accredit.fixtures;

import java.time.Instant;
import java.util.UUID;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.domain.AccreditationType;
import com.willow.wealth.accredit.query.model.Accreditation;

/**
 * Builds {@link Accreditation} read-model entities with defaults.
 * Tests only set what is required specifically
 * Example: {@code anAccreditation().withStatus(CONFIRMED).updatedAt(daysAgo(31)).build()}.
 */
public final class AccreditationTestBuilder {

	private UUID accreditationId = UUID.randomUUID();

	private String userId = TestData.USER_ID;

	private AccreditationType accreditationType = AccreditationType.BY_INCOME;

	private AccreditationStatus status = AccreditationStatus.PENDING;

	private Instant createdAt = TestData.NOW;

	private Instant updatedAt;

	private AccreditationTestBuilder() {
	}

	public static AccreditationTestBuilder anAccreditation() {
		return new AccreditationTestBuilder();
	}

	public AccreditationTestBuilder withId(UUID accreditationId) {
		this.accreditationId = accreditationId;
		return this;
	}

	public AccreditationTestBuilder forUser(String userId) {
		this.userId = userId;
		return this;
	}

	public AccreditationTestBuilder ofType(AccreditationType accreditationType) {
		this.accreditationType = accreditationType;
		return this;
	}

	public AccreditationTestBuilder withStatus(AccreditationStatus status) {
		this.status = status;
		return this;
	}

	public AccreditationTestBuilder createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	/** Defaults to {@code createdAt} when not set. */
	public AccreditationTestBuilder updatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	public Accreditation build() {
		Accreditation accreditation = Accreditation.pending(accreditationId, userId, accreditationType, createdAt);
		if (status != AccreditationStatus.PENDING || updatedAt != null) {
			accreditation.applyStatus(status, updatedAt != null ? updatedAt : createdAt);
		}
		return accreditation;
	}

}
