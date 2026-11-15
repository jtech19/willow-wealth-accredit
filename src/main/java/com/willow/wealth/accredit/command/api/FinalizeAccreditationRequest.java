package com.willow.wealth.accredit.command.api;

import jakarta.validation.constraints.NotNull;

public record FinalizeAccreditationRequest(@NotNull AccreditationOutcome outcome) {
}
