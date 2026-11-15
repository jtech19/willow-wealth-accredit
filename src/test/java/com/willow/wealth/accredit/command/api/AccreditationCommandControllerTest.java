package com.willow.wealth.accredit.command.api;

import static com.willow.wealth.accredit.domain.AccreditationType.BY_INCOME;
import static com.willow.wealth.accredit.command.api.AccreditationOutcome.FAILED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.fixtures.CreateAccreditationRequestTestBuilder.aCreateRequest;
import static com.willow.wealth.accredit.fixtures.FinalizeAccreditationRequestTestBuilder.aFinalizeRequest;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import com.willow.wealth.accredit.command.handler.AccreditationNotFoundException;
import com.willow.wealth.accredit.command.handler.CreateAccreditationHandler;
import com.willow.wealth.accredit.command.handler.FinalizeAccreditationHandler;
import com.willow.wealth.accredit.command.handler.PendingAccreditationExistsException;
import com.willow.wealth.accredit.domain.AccreditationStatus;
import com.willow.wealth.accredit.domain.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AccreditationCommandController.class)
class AccreditationCommandControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private CreateAccreditationHandler createHandler;

	@MockitoBean
	private FinalizeAccreditationHandler finalizeHandler;

	@Test
	void createReturns201WithAccreditationId() throws Exception {
		given(createHandler.create(USER_ID, BY_INCOME)).willReturn(ACCREDITATION_ID);

		postCreate(jsonMapper.writeValueAsString(aCreateRequest().build()))
			.andExpect(status().isCreated())
			.andExpect(content().json("""
					{ "accreditation_id": "%s" }
					""".formatted(ACCREDITATION_ID), JsonCompareMode.STRICT));
	}

	@Test
	void createReturns409WhenPendingAccreditationExists() throws Exception {
		given(createHandler.create(USER_ID, BY_INCOME)).willThrow(new PendingAccreditationExistsException(USER_ID));

		postCreate(jsonMapper.writeValueAsString(aCreateRequest().build()))
			.andExpect(status().isConflict());
	}

	@ParameterizedTest
	@ValueSource(strings = { "application/pdf", "image/png", "image/jpeg", "APPLICATION/PDF" })
	void allowedMimeTypesAreAccepted(String mimeType) throws Exception {
		given(createHandler.create(USER_ID, BY_INCOME)).willReturn(ACCREDITATION_ID);

		postCreate(jsonMapper.writeValueAsString(aCreateRequest().withMimeType(mimeType).build()))
			.andExpect(status().isCreated());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	void invalidRequestIsRejectedWith400(String description, Object body) throws Exception {
		String json = body instanceof String raw ? raw : jsonMapper.writeValueAsString(body);

		postCreate(json).andExpect(status().isBadRequest());

		verifyNoInteractions(createHandler);
	}

	static Stream<Arguments> invalidRequests() {
		return Stream.of(
				arguments("blank user_id", aCreateRequest().forUser(" ").build()),
				arguments("user_id over 64 chars", aCreateRequest().forUser("x".repeat(65)).build()),
				arguments("missing accreditation_type", aCreateRequest().ofType(null).build()),
				arguments("missing document", aCreateRequest().withoutDocument().build()),
				arguments("blank document name", aCreateRequest().withDocumentName("").build()),
				arguments("unsupported mime_type", aCreateRequest().withMimeType("text/plain").build()),
				arguments("empty content", aCreateRequest().withContent(new byte[0]).build()),
				arguments("content over 10 MB",
						aCreateRequest().withContent(new byte[CreateAccreditationRequest.MAX_DOCUMENT_BYTES + 1]).build()),
				arguments("content not base64", rawRequest("BY_INCOME", "not base64!")),
				arguments("content truncated base64", rawRequest("BY_INCOME", "a")),
				arguments("content bad padding", rawRequest("BY_INCOME", "YQ=x")),
				arguments("unknown accreditation_type", rawRequest("BY_LUCK", "YQ==")),
				arguments("malformed JSON", "{ not json"));
	}

	private static String rawRequest(String accreditationType, String content) {
		return """
				{
				  "user_id": "%s",
				  "accreditation_type": "%s",
				  "document": { "name": "a.pdf", "mime_type": "application/pdf", "content": "%s" }
				}
				""".formatted(USER_ID, accreditationType, content);
	}

	@Test
	void finalizeReturns200WithAccreditationId() throws Exception {
		putFinalize(ACCREDITATION_ID.toString(), jsonMapper.writeValueAsString(aFinalizeRequest().build()))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{ "accreditation_id": "%s" }
					""".formatted(ACCREDITATION_ID), JsonCompareMode.STRICT));

		verify(finalizeHandler).finalizeAccreditation(ACCREDITATION_ID, CONFIRMED);
	}

	@Test
	void finalizeReturns404WhenAccreditationUnknown() throws Exception {
		willThrow(new AccreditationNotFoundException(ACCREDITATION_ID))
			.given(finalizeHandler).finalizeAccreditation(ACCREDITATION_ID, CONFIRMED);

		putFinalize(ACCREDITATION_ID.toString(), jsonMapper.writeValueAsString(aFinalizeRequest().build()))
			.andExpect(status().isNotFound());
	}

	@Test
	void finalizeReturns409WhenTransitionNotAllowed() throws Exception {
		willThrow(new InvalidStatusTransitionException(AccreditationStatus.CONFIRMED, AccreditationStatus.FAILED))
			.given(finalizeHandler).finalizeAccreditation(ACCREDITATION_ID, AccreditationStatus.FAILED);

		putFinalize(ACCREDITATION_ID.toString(),
				jsonMapper.writeValueAsString(aFinalizeRequest().withOutcome(FAILED).build()))
			.andExpect(status().isConflict());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidFinalizeRequests")
	void invalidFinalizeRequestIsRejectedWith400(String description, String accreditationId, String body)
			throws Exception {
		putFinalize(accreditationId, body).andExpect(status().isBadRequest());

		verifyNoInteractions(finalizeHandler);
	}

	static Stream<Arguments> invalidFinalizeRequests() {
		String id = ACCREDITATION_ID.toString();
		return Stream.of(
				arguments("outcome PENDING", id, "{ \"outcome\": \"PENDING\" }"),
				arguments("unknown outcome", id, "{ \"outcome\": \"MAYBE\" }"),
				arguments("missing outcome", id, "{}"),
				arguments("malformed JSON", id, "{ not json"),
				arguments("id not a UUID", "not-a-uuid", "{ \"outcome\": \"CONFIRMED\" }"));
	}

	private ResultActions putFinalize(String accreditationId, String body) throws Exception {
		return mockMvc.perform(put("/user/accreditation/{id}", accreditationId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions postCreate(String body) throws Exception {
		return mockMvc.perform(post("/user/accreditation").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
