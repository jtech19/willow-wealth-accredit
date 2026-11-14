package com.willow.wealth.accredit.event;

import java.util.concurrent.ExecutionException;

import com.willow.wealth.accredit.config.AccreditKafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes accreditation events to Kafka.
 * <p>
 * Keyed by accreditation id, so all events of one accreditation land on the
 * same partition and stay in order. Sends are synchronous: the admin endpoint
 * only reports success once Kafka has acknowledged the event. The wait is
 * bounded by the producer's {@code delivery.timeout.ms} (application.yml).
 */
@Component
@Slf4j
public class AccreditationEventPublisher {
	private final KafkaTemplate<String, AccreditationEvent> kafkaTemplate;
	private final AccreditKafkaProperties properties;

	public AccreditationEventPublisher(KafkaTemplate<String, AccreditationEvent> kafkaTemplate,
			AccreditKafkaProperties properties) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	public void publish(AccreditationEvent event) {
		String topic = properties.topic().name();
		String key = event.accreditationId().toString();

		try {
			kafkaTemplate.send(topic, key, event).get();
			log.info("Published event {} for accreditation {} ({} -> {})", event.eventId(), key, event.previousStatus(),
					event.status());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new EventPublishException("Interrupted while publishing event for accreditation " + key, ex);
		}
		catch (ExecutionException ex) {
			throw new EventPublishException("Failed to publish event for accreditation " + key, ex);
		}
	}

}
