package com.connellboyce.authhub.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActorTypeTest {

	@Test
	void getValue_returnsExpectedValues() {
		assertEquals("identity", ActorType.IDENTITY.getValue());
		assertEquals("service", ActorType.SERVICE.getValue());
	}

	@Test
	void fromValue_matchesCaseInsensitively() {
		assertEquals(ActorType.IDENTITY, ActorType.fromValue("identity"));
		assertEquals(ActorType.IDENTITY, ActorType.fromValue("IDENTITY"));
		assertEquals(ActorType.SERVICE, ActorType.fromValue("Service"));
	}

	@Test
	void fromValue_unknownValue_throwsIllegalArgumentException() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> ActorType.fromValue("unknown"));
		assertTrue(ex.getMessage().contains("unknown"));
	}

	@Test
	void toString_returnsValue() {
		assertEquals("identity", ActorType.IDENTITY.toString());
		assertEquals("service", ActorType.SERVICE.toString());
	}
}
