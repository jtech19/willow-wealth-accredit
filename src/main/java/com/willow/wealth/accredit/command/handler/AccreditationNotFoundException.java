package com.willow.wealth.accredit.command.handler;

import java.util.UUID;

public class AccreditationNotFoundException extends RuntimeException {

	public AccreditationNotFoundException(UUID accreditationId) {
		super("Accreditation %s not found".formatted(accreditationId));
	}

}
