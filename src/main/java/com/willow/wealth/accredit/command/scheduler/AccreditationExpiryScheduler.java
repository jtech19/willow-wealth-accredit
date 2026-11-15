package com.willow.wealth.accredit.command.scheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.willow.wealth.accredit.command.handler.FinalizeAccreditationHandler;
import com.willow.wealth.accredit.config.AccreditExpiryProperties;
import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.query.model.Accreditation;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expires CONFIRMED accreditations whose last update is older than the max age (30 days).
 * Firstly the transition is validated and only then an EXPIRED event is published;
 * the projector then updates the read model.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccreditationExpiryScheduler {
	private final AccreditationRepository repository;
	private final FinalizeAccreditationHandler finalizeHandler;
	private final AccreditExpiryProperties properties;
	private final Clock clock;

	@Scheduled(cron = "${accredit.expiry.cron}")
	public void expireStaleAccreditations() {
		Instant cutoff = Instant.now(clock).minus(properties.maxAge());
		List<Accreditation> stale = repository.findByStatusAndUpdatedAtBefore(AccreditationStatus.CONFIRMED, cutoff);

		int expired = 0;
		for (Accreditation accreditation : stale) {
			try {
				finalizeHandler.finalizeAccreditation(accreditation.getAccreditationId(), AccreditationStatus.EXPIRED);
				expired++;
			}
			catch (RuntimeException ex) {
				log.warn("Could not expire accreditation {}: {}", accreditation.getAccreditationId(), ex.getMessage());
			}
		}

		log.info("Expiry run: {} of {} CONFIRMED accreditation(s) older than {} expired", expired, stale.size(), cutoff);
	}

}
