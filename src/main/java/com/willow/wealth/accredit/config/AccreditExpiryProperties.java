package com.willow.wealth.accredit.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("accredit.expiry")
public record AccreditExpiryProperties(@NotNull Duration maxAge, @NotBlank String cron) {
}
