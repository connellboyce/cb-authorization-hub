# Spec Delta

## Purpose

Defines how every management API request is authenticated and authorized under Zero Trust principles: each request proves who the developer is, what it may do, and which resources it may touch, with nothing trusted from session state or prior requests.

## ADDED Requirements

### Requirement: Bearer token only
The system SHALL authenticate management API requests (`/api/v1/clients/**`, `/api/v1/applications/**`, `/api/v1/scopes/**`) solely by an OAuth bearer access token in the `Authorization` header, and SHALL NOT accept session cookies for them.

#### Scenario: Missing token
- **WHEN** a management API request has no bearer token
- **THEN** the system responds `401` with a `WWW-Authenticate: Bearer` header

#### Scenario: Session cookie without token
- **WHEN** a request carries a valid portal session cookie but no bearer token
- **THEN** the system responds `401`

### Requirement: Stateless management requests
The system SHALL NOT create or read an HTTP session for management API requests, and SHALL NOT require a CSRF token for them.

#### Scenario: No session created
- **WHEN** an authenticated management API request succeeds
- **THEN** the response sets no session cookie

#### Scenario: No CSRF token required
- **WHEN** a bearer-authenticated `POST`, `PUT`, or `DELETE` management request omits a CSRF token
- **THEN** the request is not rejected for CSRF

### Requirement: Access token validation
The system SHALL verify each access token's signature against the hub's current signing keys and SHALL reject tokens whose issuer differs from the hub or that are expired or not yet valid.

#### Scenario: Expired token
- **WHEN** a request presents an expired access token
- **THEN** the system responds `401` with error `invalid_token`

#### Scenario: Tampered token
- **WHEN** a request presents a token whose signature does not verify
- **THEN** the system responds `401` with error `invalid_token`

### Requirement: User tokens only
The system SHALL accept only access tokens whose `sub` identifies an existing user, verified against the user store on every request.

#### Scenario: Client credentials token
- **WHEN** a request presents a token issued through the `client_credentials` grant
- **THEN** the system responds `401` with error `invalid_token`

#### Scenario: Deleted user
- **WHEN** a request presents an unexpired token whose `sub` no longer matches any user
- **THEN** the system responds `401` with error `invalid_token`

### Requirement: Developer role verified per request
The system SHALL require that the user identified by `sub` currently holds the `DEVELOPER` role in the user store at the time of each request, regardless of roles claimed in the token.

#### Scenario: Role revoked after issuance
- **WHEN** a token was issued while the user held `DEVELOPER` and the role has since been removed
- **THEN** the system responds `403`

#### Scenario: Non-developer user
- **WHEN** a user without `DEVELOPER` presents a token holding management scopes
- **THEN** the system responds `403`

### Requirement: Per-operation scopes
The system SHALL require the token to hold the scope mapped to each operation: `urn:cb:scope:authhub:clients#read` for client reads and lists; `urn:cb:scope:authhub:clients#write` for client registration, update, and delete; `urn:cb:scope:authhub:applications#read` for application and application-scope reads; `urn:cb:scope:authhub:applications#write` for application and scope creation, update, and delete; `urn:cb:scope:authhub:scopes#read` for the scope catalog.

#### Scenario: Missing scope
- **WHEN** a token without `urn:cb:scope:authhub:clients#write` is used for `POST /api/v1/clients`
- **THEN** the system responds `403` with error `insufficient_scope`

#### Scenario: Read scope does not permit writes
- **WHEN** a token holding only `urn:cb:scope:authhub:applications#read` is used for `DELETE /api/v1/applications/{id}`
- **THEN** the system responds `403` with error `insufficient_scope`

### Requirement: Object-level ownership
The system SHALL permit reading, updating, or deleting a client or application, and reading, creating, or deleting an application's scopes, only when the resource's owner equals the token's `sub`.

#### Scenario: Owner access
- **WHEN** the owner of a client requests `GET /api/v1/clients/{client_id}`
- **THEN** the request is authorized

#### Scenario: Non-owner update
- **WHEN** a developer attempts `PUT /api/v1/clients/{client_id}` on a client owned by another developer
- **THEN** the system responds `404` and the client is unchanged

### Requirement: Owner-scoped listings
The system SHALL restrict list operations for clients and applications to resources owned by the token's `sub` at query time.

#### Scenario: Other developers' resources excluded
- **WHEN** two developers each own applications and one lists applications
- **THEN** the response contains none of the other developer's applications

### Requirement: Non-owned resources indistinguishable from missing
The system SHALL respond to requests for resources the caller does not own exactly as it responds for resources that do not exist.

#### Scenario: Probing another developer's client
- **WHEN** a developer requests a `client_id` owned by someone else and a `client_id` that does not exist
- **THEN** both responses have status `404` and identical bodies

### Requirement: Ownership derived from the token
The system SHALL derive the owner of created resources from the token's `sub` and SHALL ignore any owner value supplied in a request body.

#### Scenario: Owner field in body
- **WHEN** a create request body includes an owner identifier for another user
- **THEN** the created resource is owned by the token's `sub`

### Requirement: Portal session endpoints unaffected
The system SHALL continue to serve `/portal/**` pages and `/portal/operation/**` form operations to session-authenticated users with the `DEVELOPER` role, with CSRF protection.

#### Scenario: Portal operation via session
- **WHEN** a logged-in developer submits a portal form with a valid CSRF token
- **THEN** the operation behaves as it did before this change

#### Scenario: Portal operation without CSRF
- **WHEN** a portal form operation is submitted without a CSRF token
- **THEN** the system rejects the request with `403`
