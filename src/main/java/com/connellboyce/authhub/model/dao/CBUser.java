package com.connellboyce.authhub.model.dao;

import com.connellboyce.authhub.util.CBRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@AllArgsConstructor
@Document(collection = "users")
public class CBUser {
	@Id
	private String id;
	private String username;
	private String password;
	private Set<String> roles;
	private String email;
	private String firstName;
	private String lastName;

}
