package com.willow.wealth.accredit.command.api;

import java.util.UUID;

import com.willow.wealth.accredit.command.handler.CreateAccreditationHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-facing write API. Validates, then publishes events; never writes the DB. */
@RestController
@RequiredArgsConstructor
public class AccreditationCommandController {

	private final CreateAccreditationHandler createHandler;

	@PostMapping("/user/accreditation")
	@ResponseStatus(HttpStatus.CREATED)
	public AccreditationIdResponse create(@Valid @RequestBody CreateAccreditationRequest request) {
		UUID accreditationId = createHandler.create(request.userId(), request.accreditationType());
		return new AccreditationIdResponse(accreditationId);
	}

}
