package com.willow.wealth.accredit.config;

import com.willow.wealth.accredit.event.AccreditationEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccreditKafkaProperties.class)
public class KafkaConfig {

	@Bean
	NewTopic accreditationEventsTopic(AccreditKafkaProperties properties) {
		AccreditKafkaProperties.Topic topic = properties.topic();

		return TopicBuilder.name(topic.name())
			.partitions(topic.partitions())
			.replicas(topic.replicas())
			.build();
	}

	/**
	 * Instructs Spring to use the application's value serializer (JsonMapper),
	 * so events use the same snake_case JSON as the REST API.
	 */
	@Bean
	DefaultKafkaProducerFactoryCustomizer jsonValueSerializer(JsonMapper jsonMapper) {
		return producerFactory -> useJsonValueSerializer(producerFactory, jsonMapper);
	}

	/**
	 * Reads events with the same JsonMapper (snake_case) straight into {@link AccreditationEvent}.
	 */
	@Bean
	DefaultKafkaConsumerFactoryCustomizer jsonValueDeserializer(JsonMapper jsonMapper) {
		return consumerFactory -> useJsonValueDeserializer(consumerFactory, jsonMapper);
	}

	/**
	 * Retries a failing event twice, 1s apart (e.g. DB briefly down), then logs
	 * and skips it so one bad event can't block the partition. Malformed messages
	 * are skipped straight away. See README limitations (dead-letter topic).
	 */
	@Bean
	DefaultErrorHandler kafkaErrorHandler() {
		return new DefaultErrorHandler(
			(record, ex) -> log.error("Skipping event at {}-{}@{} after retries: {}",
				record.topic(), record.partition(), record.offset(), ex.getMessage(), ex),
			new FixedBackOff(1000L, 2L));
	}

	@SuppressWarnings("unchecked")
	private static <K, V> void useJsonValueSerializer(DefaultKafkaProducerFactory<K, V> producerFactory, JsonMapper jsonMapper) {
		producerFactory.setValueSerializerSupplier(
            () -> (Serializer<V>) new JacksonJsonSerializer<>(jsonMapper).noTypeInfo());
	}

	@SuppressWarnings("unchecked")
	private static <K, V> void useJsonValueDeserializer(DefaultKafkaConsumerFactory<K, V> consumerFactory,
			JsonMapper jsonMapper) {
		consumerFactory.setValueDeserializerSupplier(() -> (Deserializer<V>) new ErrorHandlingDeserializer<>(
				new JacksonJsonDeserializer<>(AccreditationEvent.class, jsonMapper, false)));
	}

}
