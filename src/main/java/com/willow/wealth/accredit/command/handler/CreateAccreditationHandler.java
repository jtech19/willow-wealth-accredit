package com.willow.wealth.accredit.command.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.domain.AccreditationType;
import com.willow.wealth.accredit.event.AccreditationEvent;
import com.willow.wealth.accredit.event.AccreditationEventPublisher;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAccreditationHandler {
	private final AccreditationRepository repository;
	private final AccreditationEventPublisher publisher;
	private final Clock clock;

	public UUID create(String userId, AccreditationType accreditationType) {
		if (repository.existsByUserIdAndStatus(userId, AccreditationStatus.PENDING)) {
			throw new PendingAccreditationExistsException(userId);
		}

		UUID accreditationId = UUID.randomUUID();
		publisher.publish(AccreditationEvent.created(accreditationId, userId, accreditationType, Instant.now(clock)));

		log.info("Created {} accreditation {} for user {}", accreditationType, accreditationId, userId);
		return accreditationId;
	}

}
