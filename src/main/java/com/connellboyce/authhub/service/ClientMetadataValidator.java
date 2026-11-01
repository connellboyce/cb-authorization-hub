package com.connellboyce.authhub.service;

import com.connellboyce.authhub.exception.InvalidClientMetadataException;
import com.connellboyce.authhub.model.ClientMetadata;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.model.payload.request.ClientMetadataRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static com.connellboyce.authhub.exception.InvalidClientMetadataException.INVALID_CLIENT_METADATA;
import static com.connellboyce.authhub.exception.InvalidClientMetadataException.INVALID_REDIRECT_URI;

@Component
public class ClientMetadataValidator {
	private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
			AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
			AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(),
			AuthorizationGrantType.REFRESH_TOKEN.getValue()
	);
	private static final Set<String> SUPPORTED_AUTH_METHODS = Set.of(
			ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(),
			ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue()
	);
	private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");
	private static final String CLIENT_ID_PATTERN = "^[A-Za-z0-9._~-]{1,128}$";

	@Autowired
	private ScopeService scopeService;

	public ClientMetadata validateRegistration(ClientMetadataRequest request) {
		if (request.clientId() != null && !request.clientId().matches(CLIENT_ID_PATTERN)) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "client_id must be 1-128 characters of A-Z, a-z, 0-9, '.', '_', '~' or '-'");
		}
		return normalize(request);
	}

	public ClientMetadata validateUpdate(String clientId, ClientMetadataRequest request) {
		if (!clientId.equals(request.clientId())) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "client_id must match the client being updated");
		}
		return normalize(request);
	}

	private ClientMetadata normalize(ClientMetadataRequest request) {
		Set<String> grantTypes = validateGrantTypes(request.grantTypes());
		validateResponseTypes(grantTypes, request.responseTypes());
		Set<String> redirectUris = validateRedirectUris(grantTypes, request.redirectUris());
		String authMethod = validateAuthMethod(request.tokenEndpointAuthMethod());
		Set<String> scopes = validateScopes(request.scope());
		String clientName = request.clientName() == null || request.clientName().isBlank() ? null : request.clientName();

		return new ClientMetadata(clientName, redirectUris, grantTypes, authMethod, scopes);
	}

	private Set<String> validateGrantTypes(List<String> requested) {
		if (requested == null || requested.isEmpty()) {
			return Set.of(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
		}
		List<String> unsupported = requested.stream().filter(grantType -> !SUPPORTED_GRANT_TYPES.contains(grantType)).toList();
		if (!unsupported.isEmpty()) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "Unsupported grant_types: " + String.join(", ", unsupported));
		}
		return Set.copyOf(requested);
	}

	private void validateResponseTypes(Set<String> grantTypes, List<String> requested) {
		Set<String> expected = grantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue()) ? Set.of("code") : Set.of();
		if (requested != null && !Set.copyOf(requested).equals(expected)) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "response_types must be " + (expected.isEmpty() ? "empty" : "[\"code\"]") + " for the requested grant_types");
		}
	}

	private Set<String> validateRedirectUris(Set<String> grantTypes, List<String> requested) {
		List<String> redirectUris = requested == null ? List.of() : requested;
		if (grantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue()) && redirectUris.isEmpty()) {
			throw new InvalidClientMetadataException(INVALID_REDIRECT_URI, "redirect_uris is required for the authorization_code grant");
		}
		redirectUris.forEach(this::validateRedirectUri);
		return Set.copyOf(redirectUris);
	}

	private void validateRedirectUri(String redirectUri) {
		URI uri;
		try {
			uri = new URI(redirectUri);
		} catch (URISyntaxException e) {
			throw new InvalidClientMetadataException(INVALID_REDIRECT_URI, "Malformed redirect URI: " + redirectUri);
		}
		if (!uri.isAbsolute() || uri.getHost() == null) {
			throw new InvalidClientMetadataException(INVALID_REDIRECT_URI, "Redirect URI must be absolute: " + redirectUri);
		}
		if (uri.getFragment() != null) {
			throw new InvalidClientMetadataException(INVALID_REDIRECT_URI, "Redirect URI must not contain a fragment: " + redirectUri);
		}
		boolean secure = "https".equals(uri.getScheme());
		boolean loopback = "http".equals(uri.getScheme()) && LOOPBACK_HOSTS.contains(uri.getHost());
		if (!secure && !loopback) {
			throw new InvalidClientMetadataException(INVALID_REDIRECT_URI, "Redirect URI must use https unless the host is a loopback address: " + redirectUri);
		}
	}

	private String validateAuthMethod(String requested) {
		if (requested == null) {
			return ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue();
		}
		if (!SUPPORTED_AUTH_METHODS.contains(requested)) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "Unsupported token_endpoint_auth_method: " + requested);
		}
		return requested;
	}

	private Set<String> validateScopes(String requested) {
		if (requested == null || requested.isBlank()) {
			return Set.of();
		}
		Set<String> scopeNames = Arrays.stream(requested.trim().split("\\s+")).collect(Collectors.toSet());
		Set<String> existing = scopeService.getScopesByNames(scopeNames).stream().map(Scope::getName).collect(Collectors.toSet());
		List<String> unknown = scopeNames.stream().filter(name -> !existing.contains(name)).sorted().toList();
		if (!unknown.isEmpty()) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "Unknown scope: " + String.join(", ", unknown));
		}
		return scopeNames;
	}
}
