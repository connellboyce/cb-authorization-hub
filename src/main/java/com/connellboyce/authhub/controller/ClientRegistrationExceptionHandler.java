package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.exception.InvalidClientMetadataException;
import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.model.payload.response.ClientRegistrationErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.connellboyce.authhub.exception.InvalidClientMetadataException.INVALID_CLIENT_METADATA;

@RestControllerAdvice(assignableTypes = ClientRegistrationController.class)
public class ClientRegistrationExceptionHandler {

	@ExceptionHandler(InvalidClientMetadataException.class)
	public ResponseEntity<ClientRegistrationErrorResponse> handleInvalidMetadata(InvalidClientMetadataException exception) {
		return badRequest(new ClientRegistrationErrorResponse(exception.getErrorCode(), exception.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ClientRegistrationErrorResponse> handleUnreadableBody() {
		return badRequest(new ClientRegistrationErrorResponse(INVALID_CLIENT_METADATA, "Request body must be a JSON client metadata document"));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Void> handleNotFound() {
		return ResponseEntity.notFound().build();
	}

	private ResponseEntity<ClientRegistrationErrorResponse> badRequest(ClientRegistrationErrorResponse body) {
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body);
	}
}
