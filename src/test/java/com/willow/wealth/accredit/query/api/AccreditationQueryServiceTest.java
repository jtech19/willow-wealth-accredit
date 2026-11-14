package com.willow.wealth.accredit.query.api;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.FAILED;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_INCOME;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_NET_WORTH;
import static com.willow.wealth.accredit.fixtures.AccreditationTestBuilder.anAccreditation;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.OTHER_ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.OTHER_USER_ID;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static com.willow.wealth.accredit.fixtures.TestData.daysAgo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.willow.wealth.accredit.TestcontainersConfiguration;
import com.willow.wealth.accredit.query.api.UserAccreditationsResponse.AccreditationStatusView;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, AccreditationQueryService.class })
class AccreditationQueryServiceTest {

	@Autowired
	private AccreditationQueryService queryService;

	@Autowired
	private AccreditationRepository repository;

	@Test
	void returnsOnlyThatUsersAccreditationsOldestFirst() {
		repository.save(anAccreditation().withId(OTHER_ACCREDITATION_ID).ofType(BY_NET_WORTH).withStatus(CONFIRMED)
			.createdAt(daysAgo(1)).build());
		repository.save(anAccreditation().withId(ACCREDITATION_ID).ofType(BY_INCOME).withStatus(FAILED)
			.createdAt(daysAgo(2)).build());
		repository.save(anAccreditation().forUser(OTHER_USER_ID).build());
		repository.flush();

		UserAccreditationsResponse response = queryService.findByUser(USER_ID);

		assertThat(response.userId()).isEqualTo(USER_ID);
		assertThat(response.accreditationStatuses()).containsExactly(
				entry(ACCREDITATION_ID, new AccreditationStatusView(BY_INCOME, FAILED)),
				entry(OTHER_ACCREDITATION_ID,
						new AccreditationStatusView(BY_NET_WORTH, CONFIRMED)));
	}

	@Test
	void unknownUserGetsEmptyMap() {
		UserAccreditationsResponse response = queryService.findByUser("unknown-user");

		assertThat(response.userId()).isEqualTo("unknown-user");
		assertThat(response.accreditationStatuses()).isEmpty();
	}

}
