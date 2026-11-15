package com.willow.wealth.accredit.command.scheduler;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.EXPIRED;
import static com.willow.wealth.accredit.fixtures.AccreditationTestBuilder.anAccreditation;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.NOW;
import static com.willow.wealth.accredit.fixtures.TestData.OTHER_ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.daysAgo;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;

import com.willow.wealth.accredit.command.handler.FinalizeAccreditationHandler;
import com.willow.wealth.accredit.config.AccreditExpiryProperties;
import com.willow.wealth.accredit.event.EventPublishException;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccreditationExpirySchedulerTest {

	@Mock
	private AccreditationRepository repository;

	@Mock
	private FinalizeAccreditationHandler finalizeHandler;

	private AccreditationExpiryScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new AccreditationExpiryScheduler(repository, finalizeHandler,
				new AccreditExpiryProperties(Duration.ofDays(30), "0 0 * * * *"), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void expiresConfirmedAccreditationsOlderThan30Days() {
		given(repository.findByStatusAndUpdatedAtBefore(CONFIRMED, daysAgo(30))).willReturn(List.of(
				anAccreditation().withId(ACCREDITATION_ID).withStatus(CONFIRMED).updatedAt(daysAgo(31)).build(),
				anAccreditation().withId(OTHER_ACCREDITATION_ID).withStatus(CONFIRMED).updatedAt(daysAgo(45)).build()));

		scheduler.expireStaleAccreditations();

		verify(finalizeHandler).finalizeAccreditation(ACCREDITATION_ID, EXPIRED);
		verify(finalizeHandler).finalizeAccreditation(OTHER_ACCREDITATION_ID, EXPIRED);
	}

	@Test
	void oneFailureDoesNotStopTheRest() {
		given(repository.findByStatusAndUpdatedAtBefore(CONFIRMED, daysAgo(30))).willReturn(List.of(
				anAccreditation().withId(ACCREDITATION_ID).withStatus(CONFIRMED).updatedAt(daysAgo(31)).build(),
				anAccreditation().withId(OTHER_ACCREDITATION_ID).withStatus(CONFIRMED).updatedAt(daysAgo(31)).build()));
		willThrow(new EventPublishException("broker down", new RuntimeException()))
			.given(finalizeHandler).finalizeAccreditation(ACCREDITATION_ID, EXPIRED);

		scheduler.expireStaleAccreditations();

		verify(finalizeHandler).finalizeAccreditation(OTHER_ACCREDITATION_ID, EXPIRED);
	}

	@Test
	void nothingToExpireDoesNothing() {
		given(repository.findByStatusAndUpdatedAtBefore(CONFIRMED, daysAgo(30))).willReturn(List.of());

		scheduler.expireStaleAccreditations();

		verifyNoInteractions(finalizeHandler);
	}

}
