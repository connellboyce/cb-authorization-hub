# Proposal

## Why

Client, application, and scope management are only reachable through the Thymeleaf portal's session-bound form endpoints (`/portal/operation/**` and `PageController`). These are OAuth server management operations, and exposing them as token-protected RESTful APIs lets a separately deployed portal (planned as a later change) act as an ordinary OAuth client of the hub. Client registration must follow the standard Dynamic Client Registration protocol rather than a bespoke form contract.

## What Changes

- Add a Dynamic Client Registration endpoint at `POST /api/v1/clients` conforming to RFC 7591 request, response, and error formats. Callers may supply a `client_id` or let the server generate one; the server always generates the `client_secret`, which is returned only at registration.
- Add client configuration endpoints at `GET|PUT|DELETE /api/v1/clients/{client_id}` shaped per RFC 7592, plus a `GET /api/v1/clients` extension listing the caller's clients.
- Add RESTful application endpoints: list owned, create, read, replace, delete at `/api/v1/applications[/{id}]`.
- Add scope endpoints: list and create scopes under an owned application, a new scope delete endpoint, and a global scope catalog at `GET /api/v1/scopes`.
- Deleting an application that still has scopes is rejected. Deleting a scope that is assigned to any client is rejected.
- Advertise `registration_endpoint` in the authorization server metadata.
- Protect all new endpoints with a stateless, bearer-only, Zero Trust validation pipeline: JWT validation, developer user tokens only, per-request database re-verification of the user and the `DEVELOPER` role, per-operation scopes, and object-level ownership with `404` for resources the caller does not own.
- Require management scopes, already onboarded into the scopes collection by the hub owner:
  - `urn:cb:scope:authhub:clients#read`
  - `urn:cb:scope:authhub:clients#write`
  - `urn:cb:scope:authhub:applications#read`
  - `urn:cb:scope:authhub:applications#write`
  - `urn:cb:scope:authhub:scopes#read`
- Fix ownership validation so it resolves the caller from a `Jwt` principal's `sub` as well as a `UserDetails` principal.
- Move `/portal/operation/**` from the API security filter chain to the session-based UI security filter chain. The `/portal` pages and operations otherwise remain unchanged until the new portal replaces them.

## Capabilities

### New Capabilities

- `client-registration`: Registering, reading, updating, deleting, and listing OAuth clients through Dynamic Client Registration (RFC 7591) and client configuration (RFC 7592 shaped) endpoints.
- `application-management`: Managing applications and the scopes they define, including the scope catalog and the deletion rules that prevent orphaned scopes.
- `management-api-security`: Token validation, authorization, and ownership enforcement applied to every management API request.

### Modified Capabilities

None. No specs exist yet.

## Impact

- **Code**: new `@RestController`s for clients, applications, and scopes under `/api/v1`; new request/response DTOs; `ClientService`, `ApplicationService`, `ScopeService` gain owner-scoped queries, scope deletion, and reference checks; repositories gain owner-filtered and reference-count queries; `WebSecurityConfig` gains a dedicated stateless management API chain and moves `/portal/operation/**`; `AuthUtilService` / ownership validators handle `Jwt` principals.
- **APIs**: new `/api/v1/clients`, `/api/v1/applications`, `/api/v1/scopes` endpoints; `registration_endpoint` added to `/.well-known/oauth-authorization-server` and `/.well-known/openid-configuration`. Existing `/portal/**` and `POST /api/v1/user` are unchanged in behavior.
- **Data**: no schema migration required. Newly registered clients get a caller-supplied or server-generated `client_id` and an optional `client_name`; existing client documents remain readable.
- **Operations**: the five `urn:cb:scope:authhub:*` scopes exist in the scopes collection; the hub owner must assign them to any client that calls the management APIs.
- **Out of scope**: decommissioning `/portal`, the new Next.js portal, audience restriction, scope approval by API owners, persisted signing keys, and a persisted authorization store.
