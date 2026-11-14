package com.willow.wealth.accredit.command.handler;

import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_NET_WORTH;
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
import java.util.UUID;

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
class CreateAccreditationHandlerTest {

	@Mock
	private AccreditationRepository repository;

	@Mock
	private AccreditationEventPublisher publisher;

	private CreateAccreditationHandler handler;

	@BeforeEach
	void setUp() {
		handler = new CreateAccreditationHandler(repository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void publishesCreationEventAndReturnsNewId() {
		given(repository.existsByUserIdAndStatus(USER_ID, PENDING)).willReturn(false);

		UUID accreditationId = handler.create(USER_ID, BY_NET_WORTH);

		ArgumentCaptor<AccreditationEvent> captor = ArgumentCaptor.forClass(AccreditationEvent.class);
		verify(publisher).publish(captor.capture());
		AccreditationEvent event = captor.getValue();
		assertThat(event.accreditationId()).isEqualTo(accreditationId);
		assertThat(event.userId()).isEqualTo(USER_ID);
		assertThat(event.accreditationType()).isEqualTo(BY_NET_WORTH);
		assertThat(event.previousStatus()).isNull();
		assertThat(event.status()).isEqualTo(PENDING);
		assertThat(event.occurredAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsWhenUserAlreadyHasPendingAccreditation() {
		given(repository.existsByUserIdAndStatus(USER_ID, PENDING)).willReturn(true);

		assertThatThrownBy(() -> handler.create(USER_ID, BY_NET_WORTH))
			.isInstanceOf(PendingAccreditationExistsException.class);
		verify(publisher, never()).publish(any());
	}

}
