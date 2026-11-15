package com.willow.wealth.accredit.command.handler;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccreditationNotFoundException extends RuntimeException {

	public AccreditationNotFoundException(UUID accreditationId) {
		super("Accreditation %s not found".formatted(accreditationId));
	}

}
