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
 * Applies accreditation events to the read model (DB).
 * Business rules are not re-checked here; the command side validated them
 * before publishing (see README assumptions).
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
		if (isAlreadyApplied(current, event)) {
			log.info("Skipped event {} for accreditation {}: already applied or stale (current {} at {}, event {} at {})",
					event.eventId(), event.accreditationId(), current.getStatus(), current.getUpdatedAt(),
					event.status(), event.occurredAt());
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
	 * Duplicate: the row already has this status. Stale: the row was updated by a
	 * newer event (e.g. a replayed creation event after the accreditation was confirmed).
	 */
	private static boolean isAlreadyApplied(Accreditation current, AccreditationEvent event) {
		return current.getStatus() == event.status() || event.occurredAt().isBefore(current.getUpdatedAt());
	}

}
