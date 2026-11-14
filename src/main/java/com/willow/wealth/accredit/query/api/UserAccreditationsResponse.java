package com.willow.wealth.accredit.query.api;

import java.util.Map;
import java.util.UUID;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.domain.AccreditationType;

public record UserAccreditationsResponse(String userId, Map<UUID, AccreditationStatusView> accreditationStatuses) {

	public record AccreditationStatusView(AccreditationType accreditationType, AccreditationStatus status) {
	}

}
