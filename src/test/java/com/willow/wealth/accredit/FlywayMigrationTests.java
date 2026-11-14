package com.willow.wealth.accredit;

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
		jdbc.update(INSERT, "87bb6030-458e-11ed-b023-039b275a916a", "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V",
				"BY_INCOME", "PENDING");

		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM accreditation_status", Integer.class);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void invalidStatusIsRejected() {
		assertThatThrownBy(() -> jdbc.update(INSERT, "c031a5da-d59a-4d35-b1f0-0f8324dcc156", "user-1",
				"BY_INCOME", "UNKNOWN"))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_accreditation_status_status");
	}

	@Test
	void invalidAccreditationTypeIsRejected() {
		assertThatThrownBy(() -> jdbc.update(INSERT, "c031a5da-d59a-4d35-b1f0-0f8324dcc156", "user-1",
				"BY_SALARY", "PENDING"))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_accreditation_status_type");
	}

}
