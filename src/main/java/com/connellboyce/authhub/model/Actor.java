package com.connellboyce.authhub.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "sub", "typ", "act" })
public class Actor {
	private static final ObjectMapper mapper = new ObjectMapper();
	public String sub;
	public ActorType typ;
	public Actor act;

	private Actor() {
		// for deserialization
	}

	public Actor(String sub, ActorType typ) {
		this.sub = sub;
		this.typ = typ;
	}

	public Actor(String sub, ActorType typ, Actor act) {
		this.sub = sub;
		this.typ = typ;
		this.act = act;
	}

	public Actor(String actorString) {
		try {
			Actor parsed = mapper.readValue(actorString, Actor.class);
			this.sub = parsed.sub;
			this.typ = parsed.typ;
			this.act = parsed.act;
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Invalid actor string", e);
		}
	}

	@Override
	public String toString() {
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
