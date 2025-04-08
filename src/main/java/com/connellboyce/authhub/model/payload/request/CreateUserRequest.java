package com.connellboyce.authhub.model.payload.request;

import lombok.Data;

@Data
public class CreateUserRequest {
	private String username;
	private String password;
	private String email;
	private String firstName;
	private String lastName;
}
