package com.connellboyce.authhub.util;

public enum CBRole {
	ROLE_USER,
	ROLE_DEVELOPER,
	ROLE_ADMIN;

	public String withoutPrefix() {
		return this.name().replace("ROLE_", "");
	}
}
