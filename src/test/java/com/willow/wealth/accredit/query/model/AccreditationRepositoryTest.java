package com.willow.wealth.accredit.query.model;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.FAILED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_INCOME;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_NET_WORTH;
import static com.willow.wealth.accredit.fixtures.AccreditationTestBuilder.anAccreditation;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.NOW;
import static com.willow.wealth.accredit.fixtures.TestData.OTHER_USER_ID;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static com.willow.wealth.accredit.fixtures.TestData.daysAgo;
import static com.willow.wealth.accredit.fixtures.TestData.hoursLater;
import static org.assertj.core.api.Assertions.assertThat;

import com.willow.wealth.accredit.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AccreditationRepositoryTest {

	@Autowired
	private AccreditationRepository repository;

	@Autowired
	private TestEntityManager em;

	@Test
	void savesAndReadsBackAllFields() {
		repository.saveAndFlush(anAccreditation().withId(ACCREDITATION_ID).build());
		em.clear();

		Accreditation found = repository.findById(ACCREDITATION_ID).orElseThrow();

		assertThat(found.getUserId()).isEqualTo(USER_ID);
		assertThat(found.getAccreditationType()).isEqualTo(BY_INCOME);
		assertThat(found.getStatus()).isEqualTo(PENDING);
		assertThat(found.getCreatedAt()).isEqualTo(NOW);
		assertThat(found.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void applyStatusUpdatesStatusAndTimestampOnly() {
		Accreditation accreditation = repository.saveAndFlush(anAccreditation().withId(ACCREDITATION_ID).build());

		accreditation.applyStatus(CONFIRMED, hoursLater(1));
		repository.saveAndFlush(accreditation);
		em.clear();

		Accreditation found = repository.findById(ACCREDITATION_ID).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(CONFIRMED);
		assertThat(found.getUpdatedAt()).isEqualTo(hoursLater(1));
		assertThat(found.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void findByUserIdReturnsOnlyThatUsersAccreditations() {
		repository.save(anAccreditation().ofType(BY_INCOME).build());
		repository.save(anAccreditation().ofType(BY_NET_WORTH).build());
		repository.save(anAccreditation().forUser(OTHER_USER_ID).build());

		assertThat(repository.findByUserIdOrderByCreatedAtAsc(USER_ID))
			.hasSize(2)
			.allSatisfy(a -> assertThat(a.getUserId()).isEqualTo(USER_ID));
		assertThat(repository.findByUserIdOrderByCreatedAtAsc("unknown-user")).isEmpty();
	}

	@Test
	void existsByUserIdAndStatusDetectsPendingRequest() {
		repository.save(anAccreditation().withStatus(FAILED).build());

		assertThat(repository.existsByUserIdAndStatus(USER_ID, PENDING)).isFalse();

		repository.save(anAccreditation().build());

		assertThat(repository.existsByUserIdAndStatus(USER_ID, PENDING)).isTrue();
	}

	@Test
	void findByStatusAndUpdatedAtBeforeReturnsOnlyStaleMatches() {
		Accreditation stale = repository.save(anAccreditation()
			.withStatus(CONFIRMED).createdAt(daysAgo(40)).updatedAt(daysAgo(31)).build());
		repository.save(anAccreditation()
			.withStatus(CONFIRMED).createdAt(daysAgo(40)).updatedAt(daysAgo(29)).build());
		repository.save(anAccreditation()
			.withStatus(PENDING).createdAt(daysAgo(31)).build());

		assertThat(repository.findByStatusAndUpdatedAtBefore(CONFIRMED, daysAgo(30)))
			.extracting(Accreditation::getAccreditationId)
			.containsExactly(stale.getAccreditationId());
	}

}
