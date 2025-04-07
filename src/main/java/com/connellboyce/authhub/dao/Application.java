package com.connellboyce.authhub.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@Document(collection = "applications")
public class Application {
	@Id
	private String id;
	private String name;
	private String description;
	private String ownerId;
}
