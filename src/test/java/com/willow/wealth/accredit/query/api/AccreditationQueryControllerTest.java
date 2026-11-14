package com.willow.wealth.accredit.query.api;

import static com.willow.wealth.accredit.domain.AccreditationStatus.CONFIRMED;
import static com.willow.wealth.accredit.domain.AccreditationStatus.FAILED;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_INCOME;
import static com.willow.wealth.accredit.domain.AccreditationType.BY_NET_WORTH;
import static com.willow.wealth.accredit.fixtures.TestData.ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.OTHER_ACCREDITATION_ID;
import static com.willow.wealth.accredit.fixtures.TestData.USER_ID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.willow.wealth.accredit.query.api.UserAccreditationsResponse.AccreditationStatusView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccreditationQueryController.class)
class AccreditationQueryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccreditationQueryService queryService;

	@Test
	void returnsUserAccreditationsInSpecShape() throws Exception {
		Map<UUID, AccreditationStatusView> statuses = new LinkedHashMap<>();
		statuses.put(ACCREDITATION_ID, new AccreditationStatusView(BY_INCOME, FAILED));
		statuses.put(OTHER_ACCREDITATION_ID, new AccreditationStatusView(BY_NET_WORTH, CONFIRMED));
		given(queryService.findByUser(USER_ID)).willReturn(new UserAccreditationsResponse(USER_ID, statuses));

		mockMvc.perform(get("/user/{userId}/accreditation", USER_ID))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{
					  "user_id": "%s",
					  "accreditation_statuses": {
					    "%s": { "accreditation_type": "BY_INCOME", "status": "FAILED" },
					    "%s": { "accreditation_type": "BY_NET_WORTH", "status": "CONFIRMED" }
					  }
					}
					""".formatted(USER_ID, ACCREDITATION_ID, OTHER_ACCREDITATION_ID), JsonCompareMode.STRICT));
	}

	@Test
	void userWithoutAccreditationsGetsEmptyMap() throws Exception {
		given(queryService.findByUser(USER_ID)).willReturn(new UserAccreditationsResponse(USER_ID, Map.of()));

		mockMvc.perform(get("/user/{userId}/accreditation", USER_ID))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{ "user_id": "%s", "accreditation_statuses": {} }
					""".formatted(USER_ID), JsonCompareMode.STRICT));
	}

	@Test
	void userIdLongerThan64CharsIsRejected() throws Exception {
		mockMvc.perform(get("/user/{userId}/accreditation", "x".repeat(65)))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(queryService);
	}

}
