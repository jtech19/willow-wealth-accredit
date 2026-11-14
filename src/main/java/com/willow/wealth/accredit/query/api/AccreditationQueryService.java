package com.willow.wealth.accredit.query.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.willow.wealth.accredit.query.api.UserAccreditationsResponse.AccreditationStatusView;
import com.willow.wealth.accredit.query.model.Accreditation;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a user's accreditations from the MySQL read model. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccreditationQueryService {

	private final AccreditationRepository repository;

	@Transactional(readOnly = true)
	public UserAccreditationsResponse findByUser(String userId) {
		Map<UUID, AccreditationStatusView> statuses = new LinkedHashMap<>();

		List<Accreditation> userAccreditations = repository.findByUserIdOrderByCreatedAtAsc(userId);
		log.debug("Found {} accreditation(s) for user {}", userAccreditations.size(), userId);

		for (Accreditation accreditation : userAccreditations) {
			statuses.put(accreditation.getAccreditationId(),
					new AccreditationStatusView(accreditation.getAccreditationType(), accreditation.getStatus()));
		}
		return new UserAccreditationsResponse(userId, statuses);
	}

}
