package com.connellboyce.authhub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActorType {
	IDENTITY("identity"),
	SERVICE("service");

	private final String value;

	ActorType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static ActorType fromValue(String v) {
		for (ActorType t : values()) {
			if (t.value.equalsIgnoreCase(v)) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown ActorType: " + v);
	}

	@Override
	public String toString() {
		return value;
	}
}
