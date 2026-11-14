package com.willow.wealth.accredit.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of an accreditation and the transitions allowed between states.
 *
 * <pre>
 * PENDING   -> CONFIRMED | FAILED | EXPIRED
 * CONFIRMED -> EXPIRED
 * FAILED    -> (final)
 * EXPIRED   -> (final)
 * </pre>
 */
public enum AccreditationStatus {

	PENDING,
	CONFIRMED,
	EXPIRED,
	FAILED;

	public boolean canTransitionTo(AccreditationStatus target) {
		return target != null && allowedTransitions().contains(target);
	}

	public boolean isFinal() {
		return allowedTransitions().isEmpty();
	}

	/**
	 * @throws InvalidStatusTransitionException if moving to {@code target} is not allowed
	 */
	public void validateTransitionTo(AccreditationStatus target) {
		if (!canTransitionTo(target)) {
			throw new InvalidStatusTransitionException(this, target);
		}
	}

	private Set<AccreditationStatus> allowedTransitions() {
		return switch (this) {
			case PENDING -> EnumSet.of(CONFIRMED, FAILED, EXPIRED);
			case CONFIRMED -> EnumSet.of(EXPIRED);
			case EXPIRED, FAILED -> EnumSet.noneOf(AccreditationStatus.class);
		};
	}

}
