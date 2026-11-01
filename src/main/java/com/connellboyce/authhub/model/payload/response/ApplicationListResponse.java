package com.connellboyce.authhub.model.payload.response;

import java.util.List;

public record ApplicationListResponse(List<ApplicationResponse> applications) {
}
