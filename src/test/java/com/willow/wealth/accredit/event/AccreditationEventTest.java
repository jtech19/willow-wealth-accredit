package com.willow.wealth.accredit.event;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_INCOME;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.NOW;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccreditationEventTest {

	@Test
	void createdEventIsPendingWithNoPreviousStatus() {
		AccreditationEvent event = AccreditationEvent.created(ACCREDITATION_ID, USER_ID, BY_INCOME, NOW);

		assertThat(event.eventId()).isNotNull();
		assertThat(event.isCreation()).isTrue();
		assertThat(event.accreditationId()).isEqualTo(ACCREDITATION_ID);
		assertThat(event.userId()).isEqualTo(USER_ID);
		assertThat(event.accreditationType()).isEqualTo(BY_INCOME);
		assertThat(event.previousStatus()).isNull();
		assertThat(event.status()).isEqualTo(PENDING);
		assertThat(event.occurredAt()).isEqualTo(NOW);
	}

	@Test
	void statusChangedEventCarriesBothStatuses() {
		AccreditationEvent event = AccreditationEvent.statusChanged(ACCREDITATION_ID, USER_ID, BY_INCOME, PENDING,
				CONFIRMED, NOW);

		assertThat(event.isCreation()).isFalse();
		assertThat(event.previousStatus()).isEqualTo(PENDING);
		assertThat(event.status()).isEqualTo(CONFIRMED);
	}

	@Test
	void everyEventGetsItsOwnId() {
		AccreditationEvent first = AccreditationEvent.created(ACCREDITATION_ID, USER_ID, BY_INCOME, NOW);
		AccreditationEvent second = AccreditationEvent.created(ACCREDITATION_ID, USER_ID, BY_INCOME, NOW);

		assertThat(first.eventId()).isNotEqualTo(second.eventId());
	}

}
