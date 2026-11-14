package com.willow.wealth.accredit.command.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.event.AccreditationEvent;
import com.willow.wealth.accredit.event.AccreditationEventPublisher;
import com.willow.wealth.accredit.query.model.Accreditation;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Moves an accreditation to its outcome: checks it exists and the transition
 * is allowed, then publishes the status change. Never writes the DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinalizeAccreditationHandler {
	private final AccreditationRepository repository;
	private final AccreditationEventPublisher publisher;
	private final Clock clock;

	public void finalizeAccreditation(UUID accreditationId, AccreditationStatus outcome) {
		Accreditation current = repository.findById(accreditationId)
			.orElseThrow(() -> new AccreditationNotFoundException(accreditationId));

		current.getStatus().validateTransitionTo(outcome);

		publisher.publish(AccreditationEvent.statusChanged(accreditationId, current.getUserId(),
				current.getAccreditationType(), current.getStatus(), outcome, Instant.now(clock)));

		log.info("Accreditation {} moved {} -> {}", accreditationId, current.getStatus(), outcome);
	}

}
