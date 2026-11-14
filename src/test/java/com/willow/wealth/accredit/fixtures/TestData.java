package com.willow.wealth.accredit.fixtures;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Shared test values, taken from the examples in the spec where possible.
 * Values can be changed here once instead of in every test.
 */
public final class TestData {

	public static final String USER_ID = "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V";

	public static final String OTHER_USER_ID = "Xk2pQ9vLm4Rt7Wz1Bn8Cd3Fg6Hj0KsYa";

	public static final UUID ACCREDITATION_ID = UUID.fromString("87bb6030-458e-11ed-b023-039b275a916a");

	public static final UUID OTHER_ACCREDITATION_ID = UUID.fromString("c031a5da-d59a-4d35-b1f0-0f8324dcc156");

	// Deterministic NOW timestamp
	public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

	private TestData() {
	}

	public static Instant daysAgo(long days) {
		return NOW.minus(Duration.ofDays(days));
	}

	public static Instant hoursLater(long hours) {
		return NOW.plus(Duration.ofHours(hours));
	}

}
