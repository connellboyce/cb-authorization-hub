package com.connellboyce.authhub.payload.request;

import lombok.Data;

@Data
public class CreateScopeRequest {
	private String name;
	private String applicationId;
}
