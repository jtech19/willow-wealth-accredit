package com.willow.wealth.accredit;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts the same MySQL and Kafka images as docker-compose.yml for tests and
 * wires them into the app via {@link ServiceConnection}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.2"));
	}

}
