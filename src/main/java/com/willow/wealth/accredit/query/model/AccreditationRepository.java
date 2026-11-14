package com.willow.wealth.accredit.query.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.willow.wealth.accredit.domain.AccreditationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccreditationRepository extends JpaRepository<Accreditation, UUID> {

	List<Accreditation> findByUserId(String userId);

	boolean existsByUserIdAndStatus(String userId, AccreditationStatus status);

	List<Accreditation> findByStatusAndUpdatedAtBefore(AccreditationStatus status, Instant cutoff);

}
