package com.connellboyce.authhub.payload.request;

import lombok.Data;

@Data
public class CreateApplicationRequest {
	private String name;
	private String description;
}
