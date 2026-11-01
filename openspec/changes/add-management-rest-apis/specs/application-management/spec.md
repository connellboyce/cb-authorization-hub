# Spec Delta

## Purpose

Lets developers manage the applications (protected APIs) they own and the scopes those applications define, and exposes the catalog of scopes that clients may request, without ever leaving scopes orphaned.

## ADDED Requirements

### Requirement: Application representation
The system SHALL represent an application as a JSON object with `id`, `name`, and `description`, and SHALL NOT include the owner's identifier.

#### Scenario: Application document shape
- **WHEN** any application endpoint returns an application
- **THEN** the object contains `id`, `name`, and `description` and no owner field

### Requirement: List owned applications
The system SHALL return, at `GET /api/v1/applications`, an object whose `applications` array contains every application owned by the caller and no others.

#### Scenario: Caller lists applications
- **WHEN** a developer requests `GET /api/v1/applications` with `urn:cb:scope:authhub:applications#read`
- **THEN** the system responds `200` with only the applications that developer owns under `applications`

#### Scenario: Caller owns no applications
- **WHEN** a developer who owns no applications requests `GET /api/v1/applications`
- **THEN** the system responds `200` with `{"applications": []}`

### Requirement: Create application
The system SHALL create an application owned by the caller at `POST /api/v1/applications` from a JSON body with required non-blank `name` and `description`.

#### Scenario: Application created
- **WHEN** a developer posts a valid body with `urn:cb:scope:authhub:applications#write`
- **THEN** the system responds `201` with a `Location` header for the new application and the application document

#### Scenario: Missing field
- **WHEN** `name` or `description` is missing or blank
- **THEN** the system responds `400`

### Requirement: Application names are unique
The system SHALL reject creating or renaming an application to a name already used by any application.

#### Scenario: Duplicate name on create
- **WHEN** a developer creates an application whose name already exists
- **THEN** the system responds `409`

#### Scenario: Duplicate name on update
- **WHEN** a developer renames an application to a name used by a different application
- **THEN** the system responds `409`

### Requirement: Read application
The system SHALL return the application at `GET /api/v1/applications/{id}` when the caller owns it.

#### Scenario: Owner reads application
- **WHEN** the owner requests the application with `urn:cb:scope:authhub:applications#read`
- **THEN** the system responds `200` with the application document

### Requirement: Replace application
The system SHALL replace an owned application's `name` and `description` at `PUT /api/v1/applications/{id}` using the same validation as creation.

#### Scenario: Owner updates application
- **WHEN** the owner submits a valid body with `urn:cb:scope:authhub:applications#write`
- **THEN** the system responds `200` with the updated application document and the owner is unchanged

#### Scenario: Name unchanged
- **WHEN** the owner submits the application's current name with a new description
- **THEN** the update succeeds

### Requirement: Delete application without scopes
The system SHALL delete an owned application at `DELETE /api/v1/applications/{id}` only when the application defines no scopes.

#### Scenario: Application without scopes deleted
- **WHEN** the owner deletes an application that defines no scopes, with `urn:cb:scope:authhub:applications#write`
- **THEN** the system responds `204` and the application no longer exists

#### Scenario: Application with scopes
- **WHEN** the owner deletes an application that defines one or more scopes
- **THEN** the system responds `409` and the application and its scopes are unchanged

### Requirement: Scope representation
The system SHALL represent a scope as a JSON object with `id`, `name`, and `application_id`.

#### Scenario: Scope document shape
- **WHEN** an application scope endpoint returns a scope
- **THEN** the object contains `id`, `name`, and `application_id`

### Requirement: List application scopes
The system SHALL return, at `GET /api/v1/applications/{id}/scopes`, an object whose `scopes` array contains every scope defined by that owned application.

#### Scenario: Owner lists scopes
- **WHEN** the owner requests the application's scopes with `urn:cb:scope:authhub:applications#read`
- **THEN** the system responds `200` with the application's scopes under `scopes`

### Requirement: Create scope
The system SHALL create a scope on an owned application at `POST /api/v1/applications/{id}/scopes` from a JSON body with a required non-blank `name`.

#### Scenario: Scope created
- **WHEN** the owner posts a valid body with `urn:cb:scope:authhub:applications#write`
- **THEN** the system responds `201` with a `Location` header and the scope document

#### Scenario: Blank scope name
- **WHEN** `name` is missing or blank
- **THEN** the system responds `400`

### Requirement: Scope names are unique
The system SHALL reject creating a scope whose name is already used by any scope of any application.

#### Scenario: Duplicate scope name
- **WHEN** the owner creates a scope whose name already exists on any application
- **THEN** the system responds `409`

### Requirement: Delete unassigned scope
The system SHALL delete a scope at `DELETE /api/v1/applications/{id}/scopes/{scopeId}` only when the caller owns the application, the scope belongs to it, and no client of any owner is assigned the scope.

#### Scenario: Unassigned scope deleted
- **WHEN** the owner deletes a scope that no client is assigned, with `urn:cb:scope:authhub:applications#write`
- **THEN** the system responds `204` and the scope no longer appears in the application's scopes or the catalog

#### Scenario: Scope assigned to a client
- **WHEN** the owner deletes a scope that is assigned to at least one client
- **THEN** the system responds `409` and the scope is unchanged

#### Scenario: Scope belongs to a different application
- **WHEN** the scope identified by `scopeId` is not defined by the application identified by `id`
- **THEN** the system responds `404`

### Requirement: Assigned scope conflict does not disclose clients
The system SHALL report a scope-in-use conflict with at most the number of assigned clients, and SHALL NOT disclose client identifiers or owners.

#### Scenario: Conflict detail
- **WHEN** a scope deletion is rejected because three clients are assigned the scope
- **THEN** the problem detail states the scope is assigned to 3 clients and contains no client identifiers

### Requirement: Scope catalog
The system SHALL return, at `GET /api/v1/scopes`, an object whose `scopes` array lists every scope with its `name` and its application's `id` and `name`, regardless of owner.

#### Scenario: Catalog lists all scopes
- **WHEN** a developer requests `GET /api/v1/scopes` with `urn:cb:scope:authhub:scopes#read`
- **THEN** the response contains every scope across all applications, each as `{"name", "application": {"id", "name"}}`

#### Scenario: Catalog omits owner data
- **WHEN** the catalog is returned
- **THEN** no entry contains an owner identifier or application description

### Requirement: Problem detail errors
The system SHALL report application and scope endpoint errors as RFC 9457 problem details with content type `application/problem+json`.

#### Scenario: Error body shape
- **WHEN** an application or scope request fails
- **THEN** the response body contains `type`, `title`, `status`, and `detail`
