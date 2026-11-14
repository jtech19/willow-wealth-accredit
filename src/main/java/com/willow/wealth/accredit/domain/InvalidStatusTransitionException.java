package com.willow.wealth.accredit.domain;

public class InvalidStatusTransitionException extends RuntimeException {

	private final AccreditationStatus from;
	private final AccreditationStatus to;

	public InvalidStatusTransitionException(AccreditationStatus from, AccreditationStatus to) {
		super("Accreditation status cannot change from %s to %s".formatted(from, to));
		this.from = from;
		this.to = to;
	}

	public AccreditationStatus getFrom() {
		return from;
	}

	public AccreditationStatus getTo() {
		return to;
	}

}
