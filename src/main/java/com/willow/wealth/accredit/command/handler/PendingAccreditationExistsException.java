package com.willow.wealth.accredit.command.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PendingAccreditationExistsException extends RuntimeException {

	public PendingAccreditationExistsException(String userId) {
		super("User %s already has a PENDING accreditation".formatted(userId));
	}

}
