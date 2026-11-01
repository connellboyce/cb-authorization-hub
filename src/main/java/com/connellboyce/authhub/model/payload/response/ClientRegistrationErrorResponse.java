package com.connellboyce.authhub.model.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClientRegistrationErrorResponse(String error, @JsonProperty("error_description") String errorDescription) {
}
