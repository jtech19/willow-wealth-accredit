package com.willow.wealth.accredit.command.handler;

public class PendingAccreditationExistsException extends RuntimeException {

	public PendingAccreditationExistsException(String userId) {
		super("User %s already has a PENDING accreditation".formatted(userId));
	}

}
