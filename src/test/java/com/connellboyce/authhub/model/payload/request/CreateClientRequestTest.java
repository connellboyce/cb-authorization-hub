package com.connellboyce.authhub.model.payload.request;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateClientRequestTest {

    @Test
    void testToRegisteredClient_success() {
        String clientId = "my-client";
        String clientSecret = "my-secret";
        List<String> redirectUris = List.of("https://example.com/callback");
        List<String> scopes = List.of("read", "write");
        List<String> grantTypes = List.of("authorization_code", "refresh_token");

        RegisteredClient client = CreateClientRequest.toRegisteredClient(
                clientId, clientSecret, redirectUris, scopes, grantTypes
        );

        assertNotNull(client.getId());
        assertDoesNotThrow(() -> UUID.fromString(client.getId()), "ID should be a valid UUID");

        assertEquals(clientId, client.getClientId());
        assertEquals(clientSecret, client.getClientSecret());
        assertNotNull(client.getClientIdIssuedAt());
        assertTrue(client.getClientIdIssuedAt().isBefore(Instant.now().plusSeconds(5)));

        assertEquals(Set.copyOf(redirectUris), client.getRedirectUris());

        assertEquals(Set.copyOf(scopes), client.getScopes());

        Set<AuthorizationGrantType> expectedGrantTypes = Set.of(
                new AuthorizationGrantType("authorization_code"),
                new AuthorizationGrantType("refresh_token")
        );
        assertEquals(expectedGrantTypes, client.getAuthorizationGrantTypes());

        Set<ClientAuthenticationMethod> expectedAuthMethods = Set.of(
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                ClientAuthenticationMethod.CLIENT_SECRET_POST
        );
        assertEquals(expectedAuthMethods, client.getClientAuthenticationMethods());
    }

    @Test
    void testToRegisteredClient_emptyGrantTypes() {
        assertThrows(IllegalArgumentException.class, () -> {
            CreateClientRequest.toRegisteredClient(
                    "id", "secret", List.of(), List.of(), List.of()
            );
        });
    }

    @Test
    void testToRegisteredClient_authorizationCodeWithoutRedirectUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            CreateClientRequest.toRegisteredClient(
                    "id", "secret", List.of(), List.of(), List.of("authorization_code")
            );
        });
    }
}
