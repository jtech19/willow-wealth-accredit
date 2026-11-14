package com.willow.wealth.accredit.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** {@code accredit.kafka.*} settings from application.yml. */
@Validated
@ConfigurationProperties("accredit.kafka")
public record AccreditKafkaProperties(@Valid @NotNull Topic topic) {

	public record Topic(@NotBlank String name, @Min(1) int partitions, @Min(1) int replicas) {
	}

}
