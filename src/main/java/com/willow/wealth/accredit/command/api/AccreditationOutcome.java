package com.willow.wealth.accredit.command.api;

import com.willow.wealth.accredit.domain.AccreditationStatus;

public enum AccreditationOutcome {

	CONFIRMED,
	EXPIRED,
	FAILED;

	public AccreditationStatus toStatus() {
		return AccreditationStatus.valueOf(name());
	}

}
