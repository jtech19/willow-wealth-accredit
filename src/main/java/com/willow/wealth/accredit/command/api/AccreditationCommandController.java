package com.willow.wealth.accredit.command.api;

import java.util.UUID;

import com.willow.wealth.accredit.command.handler.CreateAccreditationHandler;
import com.willow.wealth.accredit.command.handler.FinalizeAccreditationHandler;
import com.willow.wealth.accredit.domain.InvalidStatusTransitionException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-facing write API. Validates, then publishes events; never writes the DB. */
@RestController
@RequiredArgsConstructor
public class AccreditationCommandController {

	private final CreateAccreditationHandler createHandler;

	private final FinalizeAccreditationHandler finalizeHandler;

	@PostMapping("/user/accreditation")
	@ResponseStatus(HttpStatus.CREATED)
	public AccreditationIdResponse create(@Valid @RequestBody CreateAccreditationRequest request) {
		UUID accreditationId = createHandler.create(request.userId(), request.accreditationType());
		return new AccreditationIdResponse(accreditationId);
	}

	@PutMapping("/user/accreditation/{accreditationId}")
	public AccreditationIdResponse finalizeAccreditation(@PathVariable UUID accreditationId,
			@Valid @RequestBody FinalizeAccreditationRequest request) {
		finalizeHandler.finalizeAccreditation(accreditationId, request.outcome().toStatus());
		return new AccreditationIdResponse(accreditationId);
	}

	// moves to exception handler in next feature
	@ExceptionHandler(InvalidStatusTransitionException.class)
	ProblemDetail invalidTransition(InvalidStatusTransitionException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

}
