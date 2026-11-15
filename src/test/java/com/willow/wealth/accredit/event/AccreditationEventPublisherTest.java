package com.willow.wealth.accredit.event;

import static com.willow.wealth.accredit.fixtures.AccreditationEventTestBuilder.aCreatedEvent;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import com.willow.wealth.accredit.config.AccreditKafkaProperties;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

/** Failure paths; the happy path is covered against real Kafka in AccreditationEventPublisherIntegrationTest. */
class AccreditationEventPublisherTest {

	@SuppressWarnings("unchecked")
	private final KafkaTemplate<String, AccreditationEvent> kafkaTemplate = mock(KafkaTemplate.class);

	private AccreditationEventPublisher publisher;

	@BeforeEach
	void setUp() {
		AccreditKafkaProperties properties = new AccreditKafkaProperties(
				new AccreditKafkaProperties.Topic("dev.willow-wealth.accredit-service.accreditation-events.v1", 1, 1));
		publisher = new AccreditationEventPublisher(kafkaTemplate, properties);
	}

	@Test
	void failedSendIsReportedAsEventPublishException() {
		when(kafkaTemplate.send(anyString(), anyString(), any()))
			.thenReturn(CompletableFuture.failedFuture(new TimeoutException("broker unavailable")));

		assertThatThrownBy(() -> publisher.publish(aCreatedEvent().build()))
			.isInstanceOf(EventPublishException.class)
			.hasMessageContaining("Failed to publish");
	}

	@Test
	void sendThatThrowsStraightAwayIsReportedAsEventPublishException() {
		when(kafkaTemplate.send(anyString(), anyString(), any()))
			.thenThrow(new KafkaException("Send failed", new TimeoutException("broker unavailable")));

		assertThatThrownBy(() -> publisher.publish(aCreatedEvent().build()))
			.isInstanceOf(EventPublishException.class)
			.hasMessageContaining("Failed to publish");
	}

}
