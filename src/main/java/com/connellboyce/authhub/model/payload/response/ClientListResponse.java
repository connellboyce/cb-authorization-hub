package com.connellboyce.authhub.model.payload.response;

import java.util.List;

public record ClientListResponse(List<ClientInformationResponse> clients) {
}
