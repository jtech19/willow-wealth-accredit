package com.willow.wealth.accredit.command.api;

import com.willow.wealth.accredit.domain.AccreditationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccreditationRequest(
		@NotBlank @Size(max = 64) String userId,
		@NotNull AccreditationType accreditationType,
		@NotNull @Valid Document document) {

	public static final int MAX_DOCUMENT_BYTES = 10 * 1024 * 1024; // max: 10MB

	/**
	 * Proof document. Jackson decodes the base64 {@code content} into bytes, so
	 * invalid base64 is rejected as unreadable JSON (400). Validated only: it
	 * isn't stored or put on the event (see README assumptions).
	 */
	public record Document(
			@NotBlank @Size(max = 255) String name,
			@NotBlank
			@Pattern(regexp = "application/pdf|image/png|image/jpeg", flags = Pattern.Flag.CASE_INSENSITIVE,
					message = "must be one of: application/pdf, image/png, image/jpeg")
			String mimeType,
			@NotEmpty @Size(max = MAX_DOCUMENT_BYTES, message = "must not exceed 10 MB") byte[] content) {
	}

}
