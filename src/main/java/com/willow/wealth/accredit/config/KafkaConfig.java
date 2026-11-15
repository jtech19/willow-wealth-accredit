package com.willow.wealth.accredit.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.ByteArrayJacksonJsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.DeserializationFeature;
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
	 * Reads events with the same JsonMapper (snake_case)
	 */
	@Bean
	RecordMessageConverter kafkaMessageConverter(JsonMapper jsonMapper) {
		return new ByteArrayJacksonJsonMessageConverter(jsonMapper.rebuild()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build());
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

}
