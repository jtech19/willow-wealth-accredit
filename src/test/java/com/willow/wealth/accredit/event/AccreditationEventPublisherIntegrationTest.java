package com.willow.wealth.accredit.event;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.fixtures.AccreditationEventTestBuilder.aStatusChangedEvent;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.willow.wealth.accredit.BaseIntegrationTest;
import com.willow.wealth.accredit.config.AccreditKafkaProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Publishes to a real Kafka broker and reads the raw record back. */
@BaseIntegrationTest
class AccreditationEventPublisherIntegrationTest {

	@Autowired
	private AccreditationEventPublisher publisher;

	@Autowired
	private AccreditKafkaProperties accreditKafkaProperties;

	@Autowired
	private KafkaProperties kafkaProperties;

	@Autowired
	private KafkaConnectionDetails connectionDetails;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void topicNameFollowsNamingStandard() {
		assertThat(accreditKafkaProperties.topic().name())
			.isEqualTo("dev.willow-wealth.accredit-service.accreditation-events.v1");
	}

	@Test
	void topicIsCreatedWithConfiguredPartitions() throws Exception {
		String topic = accreditKafkaProperties.topic().name();
		try (AdminClient admin = AdminClient.create(withBroker(kafkaProperties.buildAdminProperties()))) {
			TopicDescription description = admin.describeTopics(List.of(topic)).allTopicNames().get().get(topic);

			assertThat(description.partitions()).hasSize(accreditKafkaProperties.topic().partitions());
		}
	}

	@Test
	void publishesEventKeyedByAccreditationIdAsSnakeCaseJson() {
		AccreditationEvent event = aStatusChangedEvent().from(PENDING).to(CONFIRMED).build();

		publisher.publish(event);

		ConsumerRecord<String, String> record = readRecordForEvent(event.eventId());
		assertThat(record.key()).isEqualTo(ACCREDITATION_ID.toString());

		JsonNode json = jsonMapper.readTree(record.value());
		assertThat(json.has("creation")).as("helper method must not be serialised").isFalse();
		assertThat(json.get("accreditation_id").asString()).isEqualTo(ACCREDITATION_ID.toString());
		assertThat(json.get("user_id").asString()).isEqualTo(USER_ID);
		assertThat(json.get("accreditation_type").asString()).isEqualTo("BY_INCOME");
		assertThat(json.get("previous_status").asString()).isEqualTo("PENDING");
		assertThat(json.get("status").asString()).isEqualTo("CONFIRMED");
		assertThat(json.get("occurred_at").asString()).isEqualTo(event.occurredAt().toString());

		// published value round-trips back into the event record
		assertThat(jsonMapper.readValue(record.value(), AccreditationEvent.class)).isEqualTo(event);
	}

	private ConsumerRecord<String, String> readRecordForEvent(UUID eventId) {
		Map<String, Object> props = withBroker(kafkaProperties.buildConsumerProperties());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "publisher-it-" + UUID.randomUUID());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
			consumer.subscribe(List.of(accreditKafkaProperties.topic().name()));
			long deadline = System.currentTimeMillis() + 15_000;
			while (System.currentTimeMillis() < deadline) {
				for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
					if (record.value().contains(eventId.toString())) {
						return record;
					}
				}
			}
		}
		throw new AssertionError("Event " + eventId + " was not found on the topic");
	}

	/** KafkaProperties holds the yml address; the Testcontainers broker comes via connection details. */
	private Map<String, Object> withBroker(Map<String, Object> props) {
		props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
		return props;
	}

}
