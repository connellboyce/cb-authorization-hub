# Tasks

## 1. Security foundation

- [x] 1.1 Fix `ClientServiceImpl.validateClientOwnership` and `ApplicationServiceImpl.validateApplicationOwnership` to resolve the caller through `AuthUtilService` instead of casting to `UserDetails`; verify with new unit tests in `ClientServiceImplTest` and `ApplicationServiceImplTest` covering `UserDetails` and `Jwt` principals
- [x] 1.2 Implement `ManagementJwtAuthenticationConverter` (design D3): load the user by `sub`, throw `InvalidBearerTokenException` when absent, build authorities from current DB roles plus token `SCOPE_*`, ignore the token `role` claim; verify with unit tests for existing user, missing user, `client_credentials` style `sub`, and a revoked role
- [x] 1.3 Add the stateless management security chain (design D2) ahead of the existing API chain with the path/method/scope rules, and remove `/portal/operation/**` from the API chain; verify with MockMvc tests that cover: missing token returns `401` with `WWW-Authenticate: Bearer`; session-only requests return `401`; no session cookie is set; missing scope returns `403 insufficient_scope`; a non-developer returns `403`; and `POST /api/v1/user` is unchanged
- [x] 1.4 Verify `/portal/operation/**` now runs on the session UI chain: update `ClientsControllerTest`, `ApplicationsControllerTest`, and `ScopesControllerTest` so session plus CSRF succeeds and a request without CSRF returns `403`, and run `mvn test`

## 2. Shared API plumbing

- [x] 2.1 Add `ResourceNotFoundException`, `ResourceConflictException`, and `InvalidClientMetadataException(errorCode, description)`, and verify they compile with `mvn compile`
- [x] 2.2 Move secret generation from `PageController.generateSecret` into a shared utility that also generates `client_id` values (design D6), and point `PageController` at it; verify with a unit test for length, base64url alphabet, and uniqueness across calls, and confirm `PageControllerTest` still passes

## 3. Application and scope management

- [x] 3.1 Add owner-scoped repository and service methods: `findByIdAndOwnerId`, owner list, name-uniqueness check excluding self on update, `ScopeRepository.existsByApplicationId`, `findByNameIn`, scope delete, and `MongoRegisteredClientRepository.countByScopesContaining`; verify with unit tests in `ApplicationServiceImplTest` and `ScopeServiceImplTest` covering not-owned returns not found, duplicate names conflict, and the delete guards
- [x] 3.2 Add the application and scope DTO records (design D5) and verify JSON shape with a Jackson serialization test that asserts snake_case `application_id` and no owner field
- [x] 3.3 Implement the application controller (`GET/POST /api/v1/applications`, `GET/PUT/DELETE /api/v1/applications/{id}`); verify with MockMvc tests using `jwt()` for each application-management scenario: list shows only owned, `201` + `Location`, `400` blank fields, `409` duplicate name, `404` for not owned with a body identical to missing, `409` delete with scopes, `204` delete without
- [x] 3.4 Implement the scope endpoints (`GET/POST /api/v1/applications/{id}/scopes`, `DELETE /api/v1/applications/{id}/scopes/{scopeId}`, `GET /api/v1/scopes`); verify with MockMvc tests: create returns `201`; a duplicate name returns `409`; a scope belonging to another application returns `404`; an assigned scope returns `409` with only a count in `detail`; an unassigned scope returns `204`; the catalog lists all owners' scopes with no owner data
- [x] 3.5 Add the problem-detail `@RestControllerAdvice` limited to the application and scope controllers (design D9), and verify that error responses in 3.3 and 3.4 tests have content type `application/problem+json` with `type`, `title`, `status`, `detail`

## 4. Client registration

- [x] 4.1 Add `clientName` and `clientIdIssuedAt` to `MongoRegisteredClient` and the `ClientService` methods for create-with-owner, owner-scoped find, owner list, replace, and delete (design D6, D7); verify with `ClientServiceImplTest` cases: the secret is stored bcrypt-hashed; a find for a non-owner returns empty; replace keeps the secret, owner, `requireAuthorizationConsent`, and `useCustomTheme`
- [x] 4.2 Confirm `RegisteredClientRepositoryImpl` still maps clients with the new fields and with legacy documents lacking them, and verify with `RegisteredClientRepositoryImplTest`
- [x] 4.3 Add the client DTO records (design D5) with unknown properties ignored and null fields omitted, and verify with a Jackson test that `client_secret` is absent when null and that an unknown request field is dropped
- [x] 4.4 Implement `ClientMetadataValidator` (design D6), and verify with unit tests for every client-registration validation scenario: supplied credentials, the default grant type, an unsupported grant type, the response type default and mismatch, a missing redirect, non-loopback `http`, loopback `http`, a fragment URI, an unsupported auth method, an unknown scope, and another developer's scope accepted
- [x] 4.5 Implement the registration controller (`POST/GET /api/v1/clients`, `GET/PUT/DELETE /api/v1/clients/{client_id}`) with the RFC 7591 error advice; verify with MockMvc tests for each client-registration scenario:
  - `201` with a generated id and secret, `client_secret_expires_at` = 0, and `registration_client_uri`
  - no `registration_access_token`
  - no secret on read or list
  - PUT full replacement resets omitted fields
  - PUT with a mismatched `client_id` or any `client_secret` returns `400`
  - non-owner returns `404`
  - list shows only owned clients and returns `{"clients": []}` when there are none
  - `204` delete
- [x] 4.6 Report legacy clients with both auth methods as `client_secret_basic`, and collapse the method set on the first API update (design D7); verify with a MockMvc test using a two-method fixture

- [x] 4.7 RFC 7591/7592 compliance fixes: accept an optional caller-supplied `client_id` (format-checked, rejected if already registered) and generate one otherwise; ignore a `client_secret` on registration; accept a `client_secret` on update only when it matches the current secret; omit `scope` when a client has none. Verify with `ClientMetadataValidatorTest`, `ClientServiceImplTest`, `ClientRegistrationControllerTest`, and `ClientPayloadSerializationTest`
## 5. Discovery metadata

- [x] 5.1 Add `registration_endpoint` to authorization server metadata and OpenID provider metadata (design D10), and verify with a test against `/.well-known/oauth-authorization-server` and `/.well-known/openid-configuration` asserting the value ends in `/api/v1/clients`

- [x] 5.2 Issue and validate tokens with the hardcoded issuer `https://auth.connellboyce.com` in every environment (design D12): the shared `JwtDecoder` validates `iss`, and `registration_client_uri` is built from the issuer; verify with `ConfiguredIssuerTest` (discovery reports the issuer and a `http://localhost:2121` token is rejected) and `ManagementApiSecurityTest.foreignIssuer_returns401InvalidToken`
- [x] 5.3 Document the client id / user id collision as a vulnerability that needs a fix outside this change (design D11) and file it on GitHub; verify the issue exists ([#100](https://github.com/connellboyce/cb-authorization-hub/issues/100)) and is linked from the design and README
## 6. Documentation and manual verification

- [x] 6.1 Document the management APIs, the required `urn:cb:scope:authhub:*` scopes, the open registration-access-token decision, and the known limitations in `README.md`, and verify the README links and endpoint list match the controllers
- [x] 6.2 Add Bruno requests under `.bruno/API` for each new endpoint, using a developer user token from the Authorization Code flow, and verify each request runs successfully against a local hub with a client assigned the needed `urn:cb:scope:authhub:*` scopes

## 7. Integration check

- [x] 7.1 Run `mvn verify` and confirm all tests pass and JaCoCo coverage does not regress
- [x] 7.2 End-to-end with Bruno: create an application, add a scope, register a client that requests it, confirm deleting the scope returns `409`, update the client to drop the scope, delete the scope, delete the application, and delete the client, with each step returning the specified status

## Workflow follow-up

- Archive the change after review.
