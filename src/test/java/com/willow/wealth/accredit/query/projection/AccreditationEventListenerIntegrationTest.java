package com.willow.wealth.accredit.query.projection;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.fixtures.AccreditationEventTestBuilder.aCreatedEvent;
import static com.willow.wealth.accredit.fixtures.AccreditationEventTestBuilder.aStatusChangedEvent;
import static com.willow.wealth.accredit.fixtures.TestData.hoursLater;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.willow.wealth.accredit.TestcontainersConfiguration;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** End to end: publish to real Kafka, wait for the listener to project into real MySQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
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
	void publishedEventsAreProjectedIntoReadModel() {
		UUID id = UUID.randomUUID();

		publisher.publish(aCreatedEvent().forAccreditation(id).build());
		awaitStatus(id, PENDING);

		publisher.publish(aStatusChangedEvent().forAccreditation(id).to(CONFIRMED).occurredAt(hoursLater(1)).build());
		awaitStatus(id, CONFIRMED);
	}

	@Test
	void malformedMessageIsSkippedAndDoesNotBlockLaterEvents() throws Exception {
		UUID id = UUID.randomUUID();

		// same key => same partition, so the valid event queues up behind the bad one
		sendRaw(id.toString(), "{ invalid json");
		publisher.publish(aCreatedEvent().forAccreditation(id).build());

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
