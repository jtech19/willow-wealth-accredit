package com.willow.wealth.accredit.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

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

	@SuppressWarnings("unchecked")
	private static <K, V> void useJsonValueSerializer(DefaultKafkaProducerFactory<K, V> producerFactory, JsonMapper jsonMapper) {
		producerFactory.setValueSerializerSupplier(
            () -> (Serializer<V>) new JacksonJsonSerializer<>(jsonMapper).noTypeInfo());
	}

}
