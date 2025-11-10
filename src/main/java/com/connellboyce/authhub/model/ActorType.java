package com.connellboyce.authhub.model;

public enum ActorType {
	IDENTITY("identity"),
	SERVICE("service");

	public final String value;

	ActorType(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public String getValue() {
		return value;
	}

	public static ActorType fromValue(String v) {
		for (ActorType t : values()) {
			if (t.value.equalsIgnoreCase(v)) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown ActorType: " + v);
	}
}
