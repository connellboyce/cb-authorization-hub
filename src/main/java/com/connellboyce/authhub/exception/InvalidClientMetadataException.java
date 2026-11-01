package com.connellboyce.authhub.exception;

import lombok.Getter;

@Getter
public class InvalidClientMetadataException extends RuntimeException {
	public static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";
	public static final String INVALID_REDIRECT_URI = "invalid_redirect_uri";

	private final String errorCode;

	public InvalidClientMetadataException(String errorCode, String description) {
		super(description);
		this.errorCode = errorCode;
	}
}
