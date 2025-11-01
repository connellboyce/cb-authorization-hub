package com.connellboyce.authhub.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CBRoleTest {

    @Test
    void testWithoutPrefix() {
        assertEquals("ADMIN", CBRole.ROLE_ADMIN.withoutPrefix());
        assertEquals("USER", CBRole.ROLE_USER.withoutPrefix());
        assertEquals("DEVELOPER", CBRole.ROLE_DEVELOPER.withoutPrefix());
    }

    @Test
    void testValues() {
        assertNotNull(CBRole.values());
        assertEquals(3, CBRole.values().length);
    }

    @Test
    void testValueOf() {
        assertEquals(CBRole.ROLE_ADMIN, CBRole.valueOf("ROLE_ADMIN"));
        assertEquals(CBRole.ROLE_DEVELOPER, CBRole.valueOf("ROLE_DEVELOPER"));
        assertEquals(CBRole.ROLE_USER, CBRole.valueOf("ROLE_USER"));
    }

    @Test
    void testValueOf_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CBRole.valueOf("invalid"));
    }
}
