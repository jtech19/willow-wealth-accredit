package com.willow.wealth.accredit.config;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.willow.wealth.accredit.command.handler.AccreditationNotFoundException;
import com.willow.wealth.accredit.command.handler.PendingAccreditationExistsException;
import com.willow.wealth.accredit.domain.InvalidStatusTransitionException;
import com.willow.wealth.accredit.event.EventPublishException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(AccreditationNotFoundException.class)
	ProblemDetail notFound(AccreditationNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler({ PendingAccreditationExistsException.class, InvalidStatusTransitionException.class })
	ProblemDetail conflict(RuntimeException ex) {
		return problem(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(EventPublishException.class)
	ProblemDetail publishFailed(EventPublishException ex) {
		log.error("Event publishing failed", ex);
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "The request could not be recorded, please retry");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
	}

	// Request body failed bean validation: list every invalid field
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		List<Map<String, String>> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.sorted(Comparator.comparing(FieldError::getField))
			.map(error -> Map.of("field", toSnakeCase(error.getField()), "message",
					String.valueOf(error.getDefaultMessage())))
			.toList();

		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Request body is invalid");
		body.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	// Path variable failed validation, e.g. a user_id over 64 chars
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		List<Map<String, String>> errors = ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> Map.of("field", toSnakeCase(result.getMethodParameter().getParameterName()),
						"message", String.valueOf(error.getDefaultMessage()))))
			.toList();

		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Request parameters are invalid");
		body.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	// Body isn't JSON, has an unknown field, or a value of the wrong type (bad enum, bad base64...)
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, unreadableDetail(ex.getMostSpecificCause()));
		return ResponseEntity.badRequest().body(body);
	}

	private static String unreadableDetail(Throwable issue) {
		if (issue instanceof UnrecognizedPropertyException unknown) {
			return "Unknown field '%s'".formatted(path(unknown));
		}
		if (issue instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
			return "Invalid value for '%s'".formatted(path(jackson));
		}
		return "Request body is not valid JSON";
	}

	// JSON path of the failing field, e.g. {@code document.mime_type}
	private static String path(JacksonException ex) {
		return ex.getPath()
			.stream()
			.map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
			.collect(Collectors.joining("."));
	}

	private static String toSnakeCase(String javaPath) {
		return javaPath == null ? "" : javaPath.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
	}

	private static ProblemDetail problem(HttpStatus status, String detail) {
		return ProblemDetail.forStatusAndDetail(status, detail);
	}

}
