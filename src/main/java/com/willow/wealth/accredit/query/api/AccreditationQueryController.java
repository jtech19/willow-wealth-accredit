package com.willow.wealth.accredit.query.api;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Client-facing read API, served from the read model only. */
@RestController
@RequiredArgsConstructor
public class AccreditationQueryController {

	private final AccreditationQueryService queryService;

	@GetMapping("/user/{userId}/accreditation")
	public UserAccreditationsResponse getAccreditations(@PathVariable @Size(max = 64) String userId) {
		return queryService.findByUser(userId);
	}

}
