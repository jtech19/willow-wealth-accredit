package com.willow.wealth.accredit.command.handler;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.FAILED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_NET_WORTH;
import static com.willow.wealth.accredit.fixtures.AccreditationTestBuilder.anAccreditation;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.NOW;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import com.willow.wealth.accredit.domain.InvalidStatusTransitionException;
import com.willow.wealth.accredit.event.AccreditationEvent;
import com.willow.wealth.accredit.event.AccreditationEventPublisher;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinalizeAccreditationHandlerTest {

	@Mock
	private AccreditationRepository repository;

	@Mock
	private AccreditationEventPublisher publisher;

	private FinalizeAccreditationHandler handler;

	@BeforeEach
	void setUp() {
		handler = new FinalizeAccreditationHandler(repository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void publishesStatusChangeEvent() {
		given(repository.findById(ACCREDITATION_ID))
			.willReturn(Optional.of(anAccreditation().withId(ACCREDITATION_ID).ofType(BY_NET_WORTH).build()));

		handler.finalizeAccreditation(ACCREDITATION_ID, CONFIRMED);

		ArgumentCaptor<AccreditationEvent> captor = ArgumentCaptor.forClass(AccreditationEvent.class);
		verify(publisher).publish(captor.capture());

		AccreditationEvent event = captor.getValue();
		assertThat(event.accreditationId()).isEqualTo(ACCREDITATION_ID);
		assertThat(event.userId()).isEqualTo(USER_ID);
		assertThat(event.accreditationType()).isEqualTo(BY_NET_WORTH);
		assertThat(event.previousStatus()).isEqualTo(PENDING);
		assertThat(event.status()).isEqualTo(CONFIRMED);
		assertThat(event.occurredAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsUnknownAccreditation() {
		given(repository.findById(ACCREDITATION_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.finalizeAccreditation(ACCREDITATION_ID, CONFIRMED))
			.isInstanceOf(AccreditationNotFoundException.class);
		verify(publisher, never()).publish(any());
	}

	@Test
	void rejectsTransitionNotAllowed() {
		given(repository.findById(ACCREDITATION_ID))
			.willReturn(Optional.of(anAccreditation().withId(ACCREDITATION_ID).withStatus(FAILED).build()));

		assertThatThrownBy(() -> handler.finalizeAccreditation(ACCREDITATION_ID, CONFIRMED))
			.isInstanceOf(InvalidStatusTransitionException.class);
		verify(publisher, never()).publish(any());
	}

}
