package com.willow.wealth.accredit;

import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.OTHER_ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class FlywayMigrationTests {

	private static final String INSERT = """
			INSERT INTO accreditation_status
			    (accreditation_id, user_id, accreditation_type, status, created_at, updated_at)
			VALUES (?, ?, ?, ?, NOW(6), NOW(6))
			""";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void accreditationStatusTableIsCreated() {
		jdbc.update(INSERT, ACCREDITATION_ID.toString(), USER_ID,
				"BY_INCOME", "PENDING");

		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM accreditation_status", Integer.class);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void invalidStatusIsRejected() {
		assertThatThrownBy(() -> jdbc.update(INSERT, OTHER_ACCREDITATION_ID.toString(), USER_ID,
				"BY_INCOME", "UNKNOWN"))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_accreditation_status_status");
	}

	@Test
	void invalidAccreditationTypeIsRejected() {
		assertThatThrownBy(() -> jdbc.update(INSERT, OTHER_ACCREDITATION_ID.toString(), USER_ID,
				"BY_SALARY", "PENDING"))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_accreditation_status_type");
	}

}
