package com.willow.wealth.accredit;

import static com.willow.wealth.accredit.fixtures.CreateAccreditationRequestTestBuilder.aCreateRequest;
import static com.willow.wealth.accredit.fixtures.FinalizeAccreditationRequestTestBuilder.aFinalizeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Whole flow over real HTTP: REST API -> Kafka -> projector -> MySQL -> GET.
 * Each test uses its own user id, so tests don't see each other's data.
 */
@BaseIntegrationTest
class AccreditationFlowIntegrationTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(20);

	@LocalServerPort
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	private RestClient client;

	private String userId;

	@BeforeEach
	void setUp() {
		client = RestClient.builder()
			.baseUrl("http://localhost:" + port)
			.defaultStatusHandler(status -> true, (request, response) -> { }) // assert on 4xx instead of throwing
			.build();
		userId = "it-" + UUID.randomUUID();
	}

	@Test
	void createThenConfirmIsVisibleToClient() {
		ResponseEntity<String> created = post(aCreateRequest().forUser(userId).build());
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String accreditationId = json(created).get("accreditation_id").asString();
		awaitStatus(accreditationId, "PENDING");

		ResponseEntity<String> confirmed = client.put()
			.uri("/user/accreditation/{id}", accreditationId)
			.contentType(MediaType.APPLICATION_JSON)
			.body(jsonMapper.writeValueAsString(aFinalizeRequest().build()))
			.retrieve()
			.toEntity(String.class);
		assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
		awaitStatus(accreditationId, "CONFIRMED");
	}

	@Test
	void secondRequestWhilePendingIsRejected() {
		String accreditationId = json(post(aCreateRequest().forUser(userId).build())).get("accreditation_id").asString();
		awaitStatus(accreditationId, "PENDING");

		ResponseEntity<String> second = post(aCreateRequest().forUser(userId).build());

		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(second.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(accreditations().properties()).as("still only the first accreditation").hasSize(1);
	}

	private ResponseEntity<String> post(Object request) {
		return client.post()
			.uri("/user/accreditation")
			.contentType(MediaType.APPLICATION_JSON)
			.body(jsonMapper.writeValueAsString(request))
			.retrieve()
			.toEntity(String.class);
	}

	// The read model is eventually consistent, so wait for the projector to catch up
	private void awaitStatus(String accreditationId, String expected) {
		await().atMost(TIMEOUT).untilAsserted(() -> {
			JsonNode entry = accreditations().get(accreditationId);
			assertThat(entry).as("accreditation %s in GET response", accreditationId).isNotNull();
			assertThat(entry.get("status").asString()).isEqualTo(expected);
		});
	}

	private JsonNode accreditations() {
		ResponseEntity<String> response = client.get()
			.uri("/user/{userId}/accreditation", userId)
			.retrieve()
			.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		return json(response).get("accreditation_statuses");
	}

	private JsonNode json(ResponseEntity<String> response) {
		return jsonMapper.readTree(response.getBody());
	}

}
