package com.connellboyce.authhub.grant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdditionalGrantTypesTest {

	@Test
	void ciba_hasCorrectGrantTypeUrn() {
		assertEquals("urn:openid:params:grant-type:ciba", AdditionalGrantTypes.CIBA.getValue());
	}
}
