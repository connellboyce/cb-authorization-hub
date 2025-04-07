package com.connellboyce.authhub.model.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@Document(collection = "scopes")
public class Scope {
	@Id
	private String id;
	private String name;
	private String applicationId;
}
