package com.willow.wealth.accredit.fixtures;

import com.willow.wealth.accredit.command.api.AccreditationOutcome;
import com.willow.wealth.accredit.command.api.FinalizeAccreditationRequest;

/**
 * Builds {@link FinalizeAccreditationRequest}s, defaulting to the spec example (CONFIRMED).
 */
public final class FinalizeAccreditationRequestTestBuilder {

	private AccreditationOutcome outcome = AccreditationOutcome.CONFIRMED;

	private FinalizeAccreditationRequestTestBuilder() {
	}

	public static FinalizeAccreditationRequestTestBuilder aFinalizeRequest() {
		return new FinalizeAccreditationRequestTestBuilder();
	}

	public FinalizeAccreditationRequestTestBuilder withOutcome(AccreditationOutcome outcome) {
		this.outcome = outcome;
		return this;
	}

	public FinalizeAccreditationRequest build() {
		return new FinalizeAccreditationRequest(outcome);
	}

}
