package com.connellboyce.authhub.model.payload.request;

import lombok.Data;

@Data
public class CreateApplicationRequest {
	private String name;
	private String description;
}
