package com.willow.wealth.accredit.query.projection;

import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.fixtures.AccreditationEventTestBuilder.aCreatedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.willow.wealth.accredit.BaseIntegrationTest;
import com.willow.wealth.accredit.config.AccreditKafkaProperties;
import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.event.AccreditationEventPublisher;
import com.willow.wealth.accredit.query.model.Accreditation;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;

/**
 * Projector edge cases against real Kafka and MySQL. The normal flow is covered end to end
 * by {@code AccreditationFlowIntegrationTest}.
 */
@BaseIntegrationTest
class AccreditationEventListenerIntegrationTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(20);

	@Autowired
	private AccreditationEventPublisher publisher;

	@Autowired
	private AccreditationRepository repository;

	@Autowired
	private AccreditKafkaProperties kafkaProperties;

	@Autowired
	private KafkaConnectionDetails connectionDetails;

	@Test
	void malformedMessageIsSkippedAndDoesNotBlockLaterEvents() throws Exception {
		UUID id = UUID.randomUUID();

		// same key => same partition, so the valid event queues up behind the bad one
		sendRaw(id.toString(), "{ invalid json");
		publisher.publish(aCreatedEvent().forAccreditation(id).build());

		awaitStatus(id, PENDING);
	}

	@Test
	void eventWithUnknownFieldIsStillProjected() throws Exception {
		UUID id = UUID.randomUUID();

		/* a newer producer may add fields; the REST API rejects unknown fields where they can be fixed, the consumer
		must not, which makes it backward-compatible for event-driven systems */
		sendRaw(id.toString(), """
				{
				  "event_id": "%s",
				  "accreditation_id": "%s",
				  "user_id": "user-1",
				  "accreditation_type": "BY_INCOME",
				  "previous_status": null,
				  "status": "PENDING",
				  "occurred_at": "2026-01-15T10:00:00Z",
				  "added_in_v2": "ignored"
				}
				""".formatted(UUID.randomUUID(), id));

		awaitStatus(id, PENDING);
	}

	private void awaitStatus(UUID id, AccreditationStatus expected) {
		await().atMost(TIMEOUT).untilAsserted(() -> {
			Optional<Accreditation> row = repository.findById(id);
			assertThat(row).isPresent();
			assertThat(row.get().getStatus()).isEqualTo(expected);
		});
	}

	private void sendRaw(String key, String value) throws Exception {
		Map<String, Object> props = Map.of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers(),
				ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
				ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
			producer.send(new ProducerRecord<>(kafkaProperties.topic().name(), key, value)).get();
		}
	}

}
