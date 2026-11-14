package com.willow.wealth.accredit.fixtures;

import java.util.Base64;

import com.willow.wealth.accredit.command.api.CreateAccreditationRequest;
import com.willow.wealth.accredit.command.api.CreateAccreditationRequest.Document;
import com.willow.wealth.accredit.domain.AccreditationType;

/**
 * Builds {@link CreateAccreditationRequest}s, defaulting to the spec example.
 */
public final class CreateAccreditationRequestTestBuilder {

	private String userId = TestData.USER_ID;

	private AccreditationType accreditationType = AccreditationType.BY_INCOME;

	private String documentName = TestData.DOCUMENT_NAME;

	private String mimeType = TestData.DOCUMENT_MIME_TYPE;

	private byte[] content = Base64.getDecoder().decode(TestData.DOCUMENT_CONTENT);

	private boolean withoutDocument;

	private CreateAccreditationRequestTestBuilder() {
	}

	public static CreateAccreditationRequestTestBuilder aCreateRequest() {
		return new CreateAccreditationRequestTestBuilder();
	}

	public CreateAccreditationRequestTestBuilder forUser(String userId) {
		this.userId = userId;
		return this;
	}

	public CreateAccreditationRequestTestBuilder ofType(AccreditationType accreditationType) {
		this.accreditationType = accreditationType;
		return this;
	}

	public CreateAccreditationRequestTestBuilder withDocumentName(String documentName) {
		this.documentName = documentName;
		return this;
	}

	public CreateAccreditationRequestTestBuilder withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public CreateAccreditationRequestTestBuilder withContent(byte[] content) {
		this.content = content;
		return this;
	}

	public CreateAccreditationRequestTestBuilder withoutDocument() {
		this.withoutDocument = true;
		return this;
	}

	public CreateAccreditationRequest build() {
		Document document = withoutDocument ? null : new Document(documentName, mimeType, content);
		return new CreateAccreditationRequest(userId, accreditationType, document);
	}

}
