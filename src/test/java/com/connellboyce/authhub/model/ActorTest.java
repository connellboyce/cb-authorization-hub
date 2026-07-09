package com.connellboyce.authhub.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActorTest {

	@Test
	void constructor_subAndType_setsFields() {
		Actor actor = new Actor("client-1", ActorType.SERVICE);

		assertEquals("client-1", actor.sub);
		assertEquals(ActorType.SERVICE, actor.typ);
		assertNull(actor.act);
	}

	@Test
	void constructor_subTypeAndNestedActor_setsFields() {
		Actor nested = new Actor("client-1", ActorType.SERVICE);
		Actor actor = new Actor("client-2", ActorType.IDENTITY, nested);

		assertEquals("client-2", actor.sub);
		assertEquals(ActorType.IDENTITY, actor.typ);
		assertEquals(nested, actor.act);
	}

	@Test
	void constructor_fromJsonString_parsesFields() {
		Actor actor = new Actor("{\"sub\":\"client-1\",\"typ\":\"service\"}");

		assertEquals("client-1", actor.sub);
		assertEquals(ActorType.SERVICE, actor.typ);
		assertNull(actor.act);
	}

	@Test
	void constructor_fromInvalidJsonString_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new Actor("not-valid-json"));
	}

	@Test
	void from_nullClaim_returnsNull() {
		assertNull(Actor.from(null));
	}

	@Test
	void from_stringClaim_parsesActor() {
		Actor actor = Actor.from("{\"sub\":\"client-1\",\"typ\":\"identity\"}");

		assertNotNull(actor);
		assertEquals("client-1", actor.sub);
		assertEquals(ActorType.IDENTITY, actor.typ);
	}

	@Test
	void from_invalidStringClaim_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> Actor.from("not-valid-json"));
	}

	@Test
	void from_mapClaim_convertsToActor() {
		Actor actor = Actor.from(java.util.Map.of("sub", "client-1", "typ", "service"));

		assertNotNull(actor);
		assertEquals("client-1", actor.sub);
		assertEquals(ActorType.SERVICE, actor.typ);
	}

	@Test
	void toString_returnsJsonRepresentation() {
		Actor actor = new Actor("client-1", ActorType.SERVICE);

		String result = actor.toString();

		assertTrue(result.contains("client-1"));
		assertTrue(result.contains("service"));
	}

	@Test
	void toString_omitsNullActFieldByDefault() {
		Actor actor = new Actor("client-1", ActorType.SERVICE);

		assertFalse(actor.toString().contains("\"act\""));
	}
}
