package com.connellboyce.authhub.model.payload.request;

import lombok.Data;

@Data
public class CreateScopeRequest {
	private String name;
	private String applicationId;
}
