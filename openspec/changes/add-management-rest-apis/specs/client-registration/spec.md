# Spec Delta

## Purpose

Lets developers register and manage the OAuth clients they own through the Dynamic Client Registration protocol (RFC 7591) and RFC 7592 shaped client configuration endpoints, so any OAuth-capable tool can manage clients without a bespoke contract.

## ADDED Requirements

### Requirement: Client registration endpoint
The system SHALL accept RFC 7591 client registration requests as JSON at `POST /api/v1/clients` and SHALL record the authenticated developer as the owner of the registered client.

#### Scenario: Successful registration
- **WHEN** a developer submits valid client metadata to `POST /api/v1/clients` with a token holding `urn:cb:scope:authhub:clients#write`
- **THEN** the system responds `201 Created` with the registered client document
- **AND** the client is owned by the developer identified by the token's `sub`

### Requirement: Optional caller-supplied client identifier
The system SHALL use a `client_id` supplied in a registration request and SHALL generate one when it is omitted. A supplied `client_id` SHALL be 1-128 characters of `A-Z`, `a-z`, `0-9`, `.`, `_`, `~`, or `-`, and SHALL NOT already be registered.

#### Scenario: Client identifier generated
- **WHEN** a registration request omits `client_id`
- **THEN** the response contains a server-generated `client_id`

#### Scenario: Client identifier supplied
- **WHEN** a registration request includes `"client_id": "my-app"` and no client uses that identifier
- **THEN** the registered client's `client_id` is `my-app`

#### Scenario: Client identifier already registered
- **WHEN** a registration request includes a `client_id` that another client already uses
- **THEN** the system responds `400` with error `invalid_client_metadata`

#### Scenario: Client identifier with unsupported characters
- **WHEN** a registration request includes `"client_id": "my app"`
- **THEN** the system responds `400` with error `invalid_client_metadata`

### Requirement: Server-generated client secret
The system SHALL generate a high-entropy `client_secret` for every registration and SHALL ignore any `client_secret` supplied in the registration request.

#### Scenario: Secret generated
- **WHEN** a registration succeeds
- **THEN** the response contains a server-generated `client_secret`, `client_id_issued_at`, and `client_secret_expires_at` set to `0`

#### Scenario: Caller supplies a secret
- **WHEN** a registration request includes a `client_secret`
- **THEN** the registration proceeds and the registered secret is a different, server-generated value

### Requirement: Client secret disclosed once
The system SHALL return `client_secret` only in the registration response, and SHALL store only a one-way hash of it.

#### Scenario: Secret absent on read
- **WHEN** the owner reads or lists a registered client
- **THEN** the response does not contain `client_secret`

### Requirement: Supported client metadata
The system SHALL accept `client_name`, `redirect_uris`, `grant_types`, `response_types`, `token_endpoint_auth_method`, and `scope`. Unrecognized metadata SHALL be ignored and omitted from the response.

#### Scenario: Unknown metadata field
- **WHEN** a registration request includes a metadata field the system does not support
- **THEN** the registration proceeds and the response does not echo that field

#### Scenario: Client without scopes
- **WHEN** a client has no registered scopes
- **THEN** client documents returned for it omit `scope`

### Requirement: Grant type validation
The system SHALL accept only `authorization_code`, `client_credentials`, and `refresh_token` in `grant_types`, defaulting to `["authorization_code"]` when omitted.

#### Scenario: Default grant type
- **WHEN** a registration request omits `grant_types`
- **THEN** the registered client has `grant_types` of `["authorization_code"]`

#### Scenario: Unsupported grant type
- **WHEN** `grant_types` contains a value outside the supported set
- **THEN** the system responds `400` with error `invalid_client_metadata`

### Requirement: Response type consistency
The system SHALL require `response_types` to be `["code"]` when `grant_types` contains `authorization_code` and empty otherwise, defaulting it accordingly when omitted.

#### Scenario: Inconsistent response types
- **WHEN** `grant_types` contains `authorization_code` and `response_types` does not equal `["code"]`
- **THEN** the system responds `400` with error `invalid_client_metadata`

#### Scenario: Response types defaulted
- **WHEN** `grant_types` is `["client_credentials"]` and `response_types` is omitted
- **THEN** the registered client has `response_types` of `[]`

### Requirement: Redirect URI validation
The system SHALL require at least one redirect URI when `grant_types` contains `authorization_code`. Each redirect URI SHALL be absolute, without a fragment, and use `https` unless the host is a loopback address.

#### Scenario: Missing redirect URI
- **WHEN** `grant_types` contains `authorization_code` and `redirect_uris` is empty or omitted
- **THEN** the system responds `400` with error `invalid_redirect_uri`

#### Scenario: Insecure redirect URI
- **WHEN** a redirect URI uses `http` with a non-loopback host
- **THEN** the system responds `400` with error `invalid_redirect_uri`

#### Scenario: Loopback redirect URI
- **WHEN** a redirect URI is `http://localhost:3000/callback`
- **THEN** the redirect URI is accepted

### Requirement: Token endpoint authentication method
The system SHALL accept `client_secret_basic` or `client_secret_post` for `token_endpoint_auth_method`, defaulting to `client_secret_basic`.

#### Scenario: Unsupported authentication method
- **WHEN** `token_endpoint_auth_method` is `none` or `private_key_jwt`
- **THEN** the system responds `400` with error `invalid_client_metadata`

### Requirement: Scope validation
The system SHALL accept `scope` as a space-delimited string in which every value is an existing scope in the scope catalog.

#### Scenario: Unknown scope
- **WHEN** `scope` contains a value that does not exist in the scope catalog
- **THEN** the system responds `400` with error `invalid_client_metadata`

#### Scenario: Scope owned by another developer
- **WHEN** `scope` contains an existing scope defined by another developer's application
- **THEN** the scope is accepted

### Requirement: Registration error format
The system SHALL report client metadata errors as a JSON object containing `error` and `error_description`, as defined by RFC 7591 section 3.2.2.

#### Scenario: Error body shape
- **WHEN** a registration or update fails metadata validation
- **THEN** the response body is `{"error": "<code>", "error_description": "<detail>"}` with content type `application/json`

### Requirement: Client configuration endpoint location
The system SHALL return `registration_client_uri` set to `/api/v1/clients/{client_id}` as an absolute URL in every client document it returns.

#### Scenario: Configuration URI returned
- **WHEN** a client is registered, read, updated, or listed
- **THEN** each client document contains `registration_client_uri` pointing to that client's configuration endpoint

### Requirement: Registration access token not issued
The system SHALL NOT issue a `registration_access_token`. Client configuration requests SHALL be authorized by the owning developer's user access token. Whether to adopt RFC 7592 registration access tokens is an open decision, as is whether to use RFC 7592's `401`/`403` statuses in place of `404`.

#### Scenario: No registration access token
- **WHEN** a client is registered
- **THEN** the response does not contain `registration_access_token`

### Requirement: Read client configuration
The system SHALL return the client document at `GET /api/v1/clients/{client_id}` when the caller owns the client.

#### Scenario: Owner reads client
- **WHEN** the owner requests `GET /api/v1/clients/{client_id}` with `urn:cb:scope:authhub:clients#read`
- **THEN** the system responds `200` with the client document

### Requirement: Update client configuration
The system SHALL treat `PUT /api/v1/clients/{client_id}` as a full replacement of the client's metadata. The body SHALL contain a `client_id` matching the path. Omitted fields SHALL take their registration defaults.

#### Scenario: Owner replaces metadata
- **WHEN** the owner submits a valid full metadata document to `PUT /api/v1/clients/{client_id}` with `urn:cb:scope:authhub:clients#write`
- **THEN** the system responds `200` with the updated client document and the credentials are unchanged

#### Scenario: Omitted field is reset
- **WHEN** an update omits `grant_types`
- **THEN** the client's `grant_types` becomes `["authorization_code"]`

#### Scenario: Mismatched client identifier
- **WHEN** the body's `client_id` differs from the path's `client_id`
- **THEN** the system responds `400` with error `invalid_client_metadata`

#### Scenario: Matching secret in update body
- **WHEN** the body contains a `client_secret` equal to the client's current secret
- **THEN** the update proceeds and the secret is unchanged

#### Scenario: Mismatched secret in update body
- **WHEN** the body contains a `client_secret` that differs from the client's current secret
- **THEN** the system responds `400` with error `invalid_client_metadata` and the client is unchanged

### Requirement: Delete client
The system SHALL delete the client at `DELETE /api/v1/clients/{client_id}` when the caller owns it.

#### Scenario: Owner deletes client
- **WHEN** the owner requests `DELETE /api/v1/clients/{client_id}` with `urn:cb:scope:authhub:clients#write`
- **THEN** the system responds `204` and the client can no longer obtain tokens

### Requirement: List owned clients
The system SHALL return, at `GET /api/v1/clients`, an object whose `clients` array contains every client owned by the caller and no others.

#### Scenario: Caller lists clients
- **WHEN** a developer who owns two clients requests `GET /api/v1/clients` with `urn:cb:scope:authhub:clients#read`
- **THEN** the system responds `200` with exactly those two client documents under `clients`

#### Scenario: Caller owns no clients
- **WHEN** a developer who owns no clients requests `GET /api/v1/clients`
- **THEN** the system responds `200` with `{"clients": []}`

### Requirement: Registration endpoint advertised
The system SHALL publish `registration_endpoint` with the absolute URL of `/api/v1/clients` in its OAuth 2.0 authorization server metadata and OpenID Provider metadata.

#### Scenario: Metadata includes registration endpoint
- **WHEN** a caller requests `/.well-known/oauth-authorization-server` or `/.well-known/openid-configuration`
- **THEN** the document contains `registration_endpoint` ending in `/api/v1/clients`
