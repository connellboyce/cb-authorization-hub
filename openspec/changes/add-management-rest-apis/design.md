# Design

## Context

See proposal.md for motivation. The current state that shapes this design:

- `WebSecurityConfig` has four chains: the authorization server chain (order 1), an API chain for `/api/v1/**` and `/portal/operation/**` using JWT resource server plus `hasRole("DEVELOPER")` (order 2), a session UI chain for `/portal/**`, `/login`, `/register` (order 3), and a public/deny-all chain (order 4). The default `JwtAuthenticationConverter` only maps `scope` to `SCOPE_*` authorities, so the `role` claim the token customizer adds is never used for authorization.
- `ClientServiceImpl.validateClientOwnership` and `ApplicationServiceImpl.validateApplicationOwnership` cast the principal to `UserDetails`, which fails for `Jwt` principals. `AuthUtilServiceImpl` already resolves a user id from either principal type; for JWTs it reads `sub`, which `TokenCustomizationConfig` sets to the user's id for user-delegated tokens. For `client_credentials` tokens `sub` is the client id.
- `MongoRegisteredClient` stores `clientId`, a bcrypt `clientSecret`, sets of auth methods, grant types, redirect URIs and scopes, `ownerId`, `requireAuthorizationConsent`, `useCustomTheme`. Every client created today is registered with both `client_secret_basic` and `client_secret_post`. Client ids and secrets are currently chosen in the browser.
- `RegisteredClientRepositoryImpl.save` deliberately throws because it cannot carry an owner; client creation goes through `ClientService`.
- Scopes are referenced by clients by name. Scope and application names are globally unique by service-level checks.
- The project has no bean validation dependency; validation is done in services by throwing `IllegalArgumentException`.

## Goals / Non-Goals

**Goals:**
- Management endpoints that a separate OAuth client can call with a developer's user token, enforcing the security spec on every request.
- Ownership enforced in the data access path, so a missing ownership check cannot leak data.
- Reuse existing services and repositories; `/portal` keeps working unchanged.

**Non-Goals:**
- Changing token issuance, signing keys, or the authorization store.
- Replacing `/portal` controllers or templates.
- Pagination, filtering, or sorting of list endpoints.

## Decisions

### D1. Custom DCR controller instead of Spring Authorization Server's OIDC registration endpoint

Spring Authorization Server ships an OIDC Client Registration endpoint (`/connect/register`). It requires an initial access token with `client.create`, issues registration access tokens with `client.read`, persists through `RegisteredClientRepository.save`, and only supports OIDC metadata semantics.

Chosen: a `@RestController` at `/api/v1/clients` implementing RFC 7591 request/response/error formats directly.

- Ownership must be recorded at registration, which `RegisteredClientRepository.save` cannot do.
- Authorization must use the developer's user token with `urn:cb:scope:authhub:*` scopes rather than `client.create` / `client.read`.
- The registration access token decision is open (see Open Questions). Owning the controller keeps that decision reversible without fighting framework behavior.

Alternative considered: customizing the built-in endpoint's converters and providers. Rejected because owner binding, scope names, the token model, and the list extension would all require overriding its core behavior.

### D2. Dedicated stateless security chain for management APIs

Add a new chain ordered ahead of the existing API chain, matching `/api/v1/clients`, `/api/v1/clients/**`, `/api/v1/applications`, `/api/v1/applications/**`, `/api/v1/scopes`, `/api/v1/scopes/**`:

```
securityMatcher(management paths)
sessionManagement: STATELESS
csrf: disabled           (bearer-only, no ambient credentials)
oauth2ResourceServer.jwt(jwtAuthenticationConverter = ManagementJwtAuthenticationConverter)
authorizeHttpRequests:
  GET    /api/v1/clients/**          -> hasRole(DEVELOPER) and SCOPE_urn:cb:scope:authhub:clients#read
  POST|PUT|DELETE /api/v1/clients/** -> hasRole(DEVELOPER) and SCOPE_urn:cb:scope:authhub:clients#write
  GET    /api/v1/applications/**     -> hasRole(DEVELOPER) and SCOPE_...:applications#read
  POST|PUT|DELETE /api/v1/applications/** -> hasRole(DEVELOPER) and SCOPE_...:applications#write
  GET    /api/v1/scopes              -> hasRole(DEVELOPER) and SCOPE_...:scopes#read
  anyRequest                         -> denyAll
```

The existing API chain keeps `/api/v1/**` (in practice `POST /api/v1/user`) and drops `/portal/operation/**`. Because the UI chain already matches `/portal/**`, removing `/portal/operation/**` from the API chain is enough to move it under session authentication, the `DEVELOPER` role, and default CSRF protection.

Path-plus-method rules in the chain keep scope enforcement in one auditable place rather than spread across annotations.

### D3. Per-request user verification in the JWT authentication converter

`ManagementJwtAuthenticationConverter`:

1. Reads `sub` and loads the user by id from `UserRepository`.
2. If no user exists, throws `InvalidBearerTokenException`, producing `401 invalid_token`. This also rejects `client_credentials` tokens, whose `sub` is a client id, and tokens for deleted users.
3. Builds authorities from the user's **current** roles in the database (`ROLE_*`), plus `SCOPE_*` from the token's `scope` claim. The token's `role` claim is ignored.
4. Returns a `JwtAuthenticationToken` whose name is the user id.

Alternatives considered:
- Mapping the `role` claim from the token: rejected because a revoked role would remain effective until expiry.
- Caching user lookups: rejected for this change because any cache window weakens per-request verification. The cost is one indexed lookup by `_id` per request.

### D4. Ownership enforced by owner-scoped queries, not `@PreAuthorize`

New service methods take the caller's user id and query with it (for example `findByClientIdAndOwnerId`, `findByIdAndOwnerId`). An empty result throws a not-found exception mapped to `404`. This gives:

- identical responses for "missing" and "not yet owned" by construction,
- owner filtering at query time for lists,
- no separate check that can be forgotten.

`@PreAuthorize` returns `403`, which would reveal existence, so the new controllers do not use it. The existing ownership validators used by `/portal/operation/**` are fixed to resolve the caller through `AuthUtilService` instead of casting to `UserDetails`, so they are safe for either principal type.

### D5. API representations as dedicated DTOs

Records under `model/payload` with explicit JSON names, never DAOs:

- `ClientMetadataRequest`: `client_id` (optional on registration, required to match on update), `client_secret` (ignored on registration; on update it must match the current secret), `client_name`, `redirect_uris`, `grant_types`, `response_types`, `token_endpoint_auth_method`, `scope`. Unknown properties are ignored.
- `ClientInformationResponse`: `client_id`, `client_secret` (registration only, omitted when null), `client_id_issued_at`, `client_secret_expires_at` (`0`), `registration_client_uri`, `client_name`, `redirect_uris`, `grant_types`, `response_types`, `token_endpoint_auth_method`, `scope` (omitted when the client has no scopes).
- `ClientListResponse { clients }`, `ApplicationRequest { name, description }`, `ApplicationResponse { id, name, description }`, `ApplicationListResponse { applications }`, `ScopeRequest { name }`, `ScopeResponse { id, name, application_id }`, `ScopeListResponse { scopes }`, `ScopeCatalogResponse { scopes: [{ name, application: { id, name } }] }`.

`response_types` is not stored. It is derived from `grant_types` on output and validated against it on input.

### D6. Client registration processing

A `ClientMetadataValidator` applies the client-registration spec's rules in a fixed order and throws `InvalidClientMetadataException(errorCode, description)`. The order is: credentials present, grant types, response types, redirect URIs, auth method, then scopes. Scope existence is checked with a single `findByNameIn` query against the scope collection.

Registration then:
- uses the caller's `client_id` when supplied, after checking its format (1-128 characters of `A-Z a-z 0-9 . _ ~ -`, since it appears in `registration_client_uri`) and that no client already uses it; otherwise generates one as 16 random bytes, base64url without padding, retrying on collision,
- always generates `client_secret`, ignoring any value in the request, as 32 random bytes from `SecureRandom`, base64url without padding, reusing the logic currently in `PageController.generateSecret` (moved to a shared utility),
- persists through `ClientService` with the bcrypt-encoded secret, the owner id, the single chosen auth method, `clientName`, and `clientIdIssuedAt`,
- returns the plaintext secret once.

Update replaces all metadata fields per RFC 7592. The id, secret, owner, `requireAuthorizationConsent`, and `useCustomTheme` are preserved. A `client_secret` in the update body is compared to the stored bcrypt hash after ownership is established, so non-owners get `404` and can't use it to test secrets; a mismatch is `400 invalid_client_metadata`, as RFC 7592 section 2.2 requires.

### D7. Additive client document fields

`MongoRegisteredClient` gains `clientName` (nullable) and `clientIdIssuedAt` (nullable `Instant`). Existing documents remain valid. The response omits `client_id_issued_at` when it is absent.

For legacy clients that store both auth methods, `token_endpoint_auth_method` is reported as `client_secret_basic`. The first API update collapses the stored set to the single value submitted. The `/portal` create path keeps registering both methods.

### D8. Deletion guards

- Application delete: `ScopeRepository.existsByApplicationId(id)`. If true, respond `409`.
- Scope delete: verify the scope's `applicationId` matches the owned application (else `404`), then `MongoRegisteredClientRepository.countByScopesContaining(name)`. If greater than zero, respond `409` with a problem detail stating only the count.

### D9. Error mapping

Two `@RestControllerAdvice` classes, each limited to its controllers with `assignableTypes`:

- Client controller advice: `InvalidClientMetadataException` maps to `400 {"error","error_description"}`. Not-found maps to an empty `404`.
- Application/scope controller advice: maps to `ProblemDetail` with `400` (validation), `404` (not found or not owned), and `409` (name conflict, has scopes, scope in use).

Domain exceptions (`ResourceNotFoundException`, `ResourceConflictException`) replace the bare `IllegalArgumentException` and `null` returns in the code paths the new controllers use. Existing `/portal` code paths keep their current behavior.

### D10. Advertising the registration endpoint

Customize `OAuth2AuthorizationServerConfigurer` with `authorizationServerMetadataEndpoint(...).authorizationServerMetadataCustomizer(...)` and the OIDC `providerConfigurationEndpoint(...).providerConfigurationCustomizer(...)`, each calling `clientRegistrationEndpoint(issuer + "/api/v1/clients")`. The issuer comes from `AuthorizationServerContextHolder`, which is available inside the authorization server chain.

`registration_client_uri` is built from the configured issuer (D12) in `AuthorizationServerSettings`, because the authorization server context is not populated on the management chain. Using the issuer rather than the request URL keeps the value correct behind a proxy.

### D11. The `username` claim must match the user identified by `sub`

`ManagementJwtAuthenticationConverter` resolves the user by `sub` and also requires the token's `username` claim to equal that user's username. `sub` alone cannot tell a user token from a client token: `client_credentials` tokens use the `client_id` as `sub`, and both the portal and the registration endpoint let developers choose a `client_id`, including a value equal to a user's id. Only user-delegated tokens carry `username` (set by `TokenCustomizationConfig`), and token exchange copies it from the subject token, so client tokens are rejected even when their `sub` collides with a user id.

This is a mitigation scoped to the management API, not a fix. The root cause, caller-chosen client ids that can collide with user ids, still exposes any other resource server that resolves the caller from `sub` alone. It needs to be fixed and is tracked in [#100](https://github.com/connellboyce/cb-authorization-hub/issues/100).

Alternative considered: rejecting tokens whose `sub` matches an existing `client_id`. Rejected because it depends on the client collection, which the attacker controls, rather than on a claim only the hub writes for users.

### D12. Hardcoded issuer for issuing and validating tokens

`AuthorizationServerSettings` uses the constant issuer `https://auth.connellboyce.com` in every environment, local included, instead of deriving it per request.

The shared `JwtDecoder` bean validates `iss` against that issuer (`JwtValidators.createDefaultWithIssuer`). That applies to every resource-server chain, including `/userinfo`, the management API, and token exchange subject tokens, not only the management API. A token whose `iss` is not the issuer is rejected with `401 invalid_token`.

Alternatives considered:
- Deriving the expected issuer from each request (the first implementation): rejected because the API would reject valid tokens when reached through a different hostname than the one that issued them, such as an internal service address.
- A configurable issuer (`localhost` locally, production when deployed): rejected in favor of one fixed value with no environment-specific configuration to get wrong.

## Risks / Trade-offs

- [Registration access token decision is open: the API deviates from RFC 7592, which requires a `registration_access_token` in responses] -> Documented in the client-registration spec and below. The controller owns token handling, so adopting a RAT later is additive.
- [Any developer can request another developer's scope, and so can block deletion of that scope and its application indefinitely] -> Accepted until the scope approval change. The `409` reveals only a count.
- [Check-then-delete race: a client may be assigned a scope between the reference count and the delete, leaving an orphan] -> Accepted. The window is milliseconds and registration only accepts existing scopes. A transactional guard can follow if Mongo runs as a replica set.
- [Deleted scope names can be reused by another application, and tokens issued before deletion still carry the name] -> Accepted until the persisted authorization store change. Today a restart clears all authorizations, and the signing key rotates on boot.
- [The legacy client auth method set collapses to one value on the first API update] -> Documented in D7. Clients using the other method must re-register or send the method they use.
- [Caller-chosen client ids can collide with user ids, letting `client_credentials` tokens pose as users to resource servers that trust `sub` alone] -> Mitigated for the management API by D11. **Needs a fix outside this change**, tracked in [#100](https://github.com/connellboyce/cb-authorization-hub/issues/100).
- [A locally running hub issues tokens for, and advertises discovery endpoints at, `https://auth.connellboyce.com`] -> Accepted. Local clients call the local endpoints directly instead of relying on discovery. Local and production signing keys differ (generated at startup), so tokens cannot cross environments today; once keys are persisted, local and production must not share a key.
- [Changing the issuer invalidates every outstanding token, because `iss` no longer matches] -> Acceptable today: the signing key already rotates on every restart, which invalidates tokens anyway.
- [One user lookup per management request] -> Indexed primary-key read. Acceptable for management traffic volume.

## Migration Plan

1. Deploy. All changes are additive, and `/portal` keeps working. Confirm `/.well-known/oauth-authorization-server` reports the `https://auth.connellboyce.com` issuer and `registration_endpoint` after rollout.
2. The five `urn:cb:scope:authhub:*` scopes already exist in the scopes collection (onboarded before deployment).
3. Hub owner assigns the needed scopes to a client used for testing (Bruno) and later to the new portal's client.
4. Rollback: redeploy the previous version. New document fields are ignored by the old code, and clients registered through the API remain usable.

## Open Questions

- **Registration access tokens (RFC 7592).** Should management requests require a per-client registration access token, the owner's user token (current interim behavior), or both? The current behavior is interim and is not a settled decision. The same decision settles status codes: RFC 7592 returns `401` for an unknown client and `403` for a forbidden one, while the API returns `404` for both so callers can't tell the cases apart.
- **Token exchange tokens.** Should tokens minted through token exchange (carrying an `act` claim) be accepted by the management API when their `sub` is a developer? Under the current rules they are accepted, because token exchange copies `sub` and `username` from the subject token, and only those claims, roles, and scopes are evaluated.
