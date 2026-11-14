package com.willow.wealth.accredit.domain;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.EXPIRED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.FAILED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class AccreditationStatusTest {

	@ParameterizedTest(name = "{0} -> {1} allowed")
	@CsvSource({
			"PENDING, CONFIRMED",
			"PENDING, FAILED",
			"PENDING, EXPIRED",
			"CONFIRMED, EXPIRED"
	})
	void allowedTransitions(AccreditationStatus from, AccreditationStatus to) {
		assertThat(from.canTransitionTo(to)).isTrue();
		assertThatNoException().isThrownBy(() -> from.validateTransitionTo(to));
	}

	@ParameterizedTest(name = "{0} -> {1} rejected")
	@CsvSource({
			"PENDING, PENDING",
			"CONFIRMED, PENDING",
			"CONFIRMED, CONFIRMED",
			"CONFIRMED, FAILED",
			"FAILED, PENDING",
			"FAILED, CONFIRMED",
			"FAILED, EXPIRED",
			"FAILED, FAILED",
			"EXPIRED, PENDING",
			"EXPIRED, CONFIRMED",
			"EXPIRED, FAILED",
			"EXPIRED, EXPIRED"
	})
	void rejectedTransitions(AccreditationStatus from, AccreditationStatus to) {
		assertThat(from.canTransitionTo(to)).isFalse();
		assertThatThrownBy(() -> from.validateTransitionTo(to))
			.isInstanceOf(InvalidStatusTransitionException.class)
			.hasMessage("Accreditation status cannot change from %s to %s", from, to);
	}

	@ParameterizedTest
	@EnumSource(AccreditationStatus.class)
	void nullTargetIsRejected(AccreditationStatus from) {
		assertThat(from.canTransitionTo(null)).isFalse();
	}

	@Test
	void onlyFailedAndExpiredAreFinal() {
		assertThat(FAILED.isFinal()).isTrue();
		assertThat(EXPIRED.isFinal()).isTrue();
		assertThat(PENDING.isFinal()).isFalse();
		assertThat(CONFIRMED.isFinal()).isFalse();
	}

}
