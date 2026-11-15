package com.willow.wealth.accredit.query.projection;

import java.util.Optional;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.event.AccreditationEvent;
import com.willow.wealth.accredit.query.model.Accreditation;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies accreditation events to the read model (DB)
 */
@Slf4j
@Service
public class AccreditationProjector {

	private final AccreditationRepository repository;

	public AccreditationProjector(AccreditationRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void apply(AccreditationEvent event) {
		Optional<Accreditation> existing = repository.findById(event.accreditationId());

		if (existing.isEmpty()) {
			repository.save(newAccreditation(event));
			log.info("Projected new accreditation {} for user {} as {}", event.accreditationId(), event.userId(), event.status());
			return;
		}

		Accreditation current = existing.get();
		if (!isApplicable(current, event)) {
			log.info("Skipped event {} for accreditation {}: current status is {}, event is {} -> {}", event.eventId(),
					event.accreditationId(), current.getStatus(), event.previousStatus(), event.status());
			return;
		}

		current.applyStatus(event.status(), event.occurredAt());
		log.info("Projected accreditation {}: {} -> {}", event.accreditationId(), event.previousStatus(), event.status());
	}

	private static Accreditation newAccreditation(AccreditationEvent event) {
		Accreditation accreditation = Accreditation.pending(event.accreditationId(), event.userId(),
				event.accreditationType(), event.occurredAt());

		if (event.status() != AccreditationStatus.PENDING) {
			accreditation.applyStatus(event.status(), event.occurredAt());
		}

		return accreditation;
	}

	/**
	 * Applies only if the event was decided on an accredit's current status and the move
	 * is allowed. Duplicates and replays fail this, since a status is never revisited.
	 * Order comes from Kafka (same key, same partition)
	 */
	private static boolean isApplicable(Accreditation current, AccreditationEvent event) {
		return event.previousStatus() == current.getStatus() && current.getStatus().canTransitionTo(event.status());
	}

}
