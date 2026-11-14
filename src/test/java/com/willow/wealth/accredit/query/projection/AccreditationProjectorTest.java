package com.willow.wealth.accredit.query.projection;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.EXPIRED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.PENDING;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_NET_WORTH;
import static com.willow.wealth.accredit.fixtures.AccreditationEventTestBuilder.aCreatedEvent;
import static com.willow.wealth.accredit.fixtures.AccreditationEventTestBuilder.aStatusChangedEvent;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.NOW;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static com.willow.wealth.accredit.fixtures.TestData.hoursLater;
import static org.assertj.core.api.Assertions.assertThat;

import com.willow.wealth.accredit.TestcontainersConfiguration;
import com.willow.wealth.accredit.event.AccreditationEvent;
import com.willow.wealth.accredit.query.model.Accreditation;
import com.willow.wealth.accredit.query.model.AccreditationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, AccreditationProjector.class })
class AccreditationProjectorTest {

	@Autowired
	private AccreditationProjector projector;

	@Autowired
	private AccreditationRepository repository;

	@Autowired
	private TestEntityManager em;

	@Test
	void creationEventInsertsPendingRow() {
		apply(aCreatedEvent().ofType(BY_NET_WORTH).build());

		Accreditation row = row();
		assertThat(row.getUserId()).isEqualTo(USER_ID);
		assertThat(row.getAccreditationType()).isEqualTo(BY_NET_WORTH);
		assertThat(row.getStatus()).isEqualTo(PENDING);
		assertThat(row.getCreatedAt()).isEqualTo(NOW);
		assertThat(row.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void statusChangeUpdatesStatusAndTimestamp() {
		apply(aCreatedEvent().build());
		apply(aStatusChangedEvent().from(PENDING).to(CONFIRMED).occurredAt(hoursLater(1)).build());

		Accreditation row = row();
		assertThat(row.getStatus()).isEqualTo(CONFIRMED);
		assertThat(row.getUpdatedAt()).isEqualTo(hoursLater(1));
		assertThat(row.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void duplicateCreationEventIsIgnored() {
		AccreditationEvent created = aCreatedEvent().build();
		apply(created);
		apply(created);

		assertThat(repository.count()).isEqualTo(1);
		assertThat(row().getStatus()).isEqualTo(PENDING);
	}

	@Test
	void duplicateStatusChangeIsIgnored() {
		apply(aCreatedEvent().build());
		apply(aStatusChangedEvent().to(CONFIRMED).occurredAt(hoursLater(1)).build());
		apply(aStatusChangedEvent().to(CONFIRMED).occurredAt(hoursLater(2)).build());

		assertThat(row().getUpdatedAt()).as("first CONFIRMED wins").isEqualTo(hoursLater(1));
	}

	@Test
	void replayedCreationEventDoesNotRevertNewerStatus() {
		AccreditationEvent created = aCreatedEvent().build();
		apply(created);
		apply(aStatusChangedEvent().to(CONFIRMED).occurredAt(hoursLater(1)).build());
		apply(created);

		assertThat(row().getStatus()).isEqualTo(CONFIRMED);
	}

	@Test
	void olderStatusChangeIsIgnored() {
		apply(aCreatedEvent().build());
		apply(aStatusChangedEvent().from(PENDING).to(CONFIRMED).occurredAt(hoursLater(2)).build());
		apply(aStatusChangedEvent().from(PENDING).to(EXPIRED).occurredAt(hoursLater(1)).build());

		assertThat(row().getStatus()).isEqualTo(CONFIRMED);
	}

	@Test
	void statusChangeForUnknownAccreditationCreatesRow() {
		apply(aStatusChangedEvent().from(PENDING).to(CONFIRMED).occurredAt(hoursLater(1)).build());

		Accreditation row = row();
		assertThat(row.getStatus()).isEqualTo(CONFIRMED);
		assertThat(row.getUserId()).isEqualTo(USER_ID);
		assertThat(row.getUpdatedAt()).isEqualTo(hoursLater(1));
	}

	/** Applies the event and flushes, so the next read comes from MySQL, not the JPA cache. */
	private void apply(AccreditationEvent event) {
		projector.apply(event);
		em.flush();
		em.clear();
	}

	private Accreditation row() {
		return repository.findById(ACCREDITATION_ID).orElseThrow();
	}

}
