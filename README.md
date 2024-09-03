# OAuth Server and Client Portal
### An OAuth 2.1 implementation with Spring Authorization Server and a Spring Web based portal to manage clients and scopes.

<hr>

## OAuth 2.1
The OAuth 2.1 Authorization Framework ([proposal](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-10)) is an extension of the OAuth 2.0 standard ([RFC6749](https://datatracker.ietf.org/doc/html/rfc6749)) authorization server. 
### 2.0 vs 2.1 Major Changes
- Implicit grant is deprecated and should not be used. This authorization server will not include the implicit grant.
- Password grant is deprecated and should not be used. This authorization server will not include the password grant.
- Authorization Code grant now utilizes PKCE (Proof-of-Key-Code-Exchange, [RFC7636](https://datatracker.ietf.org/doc/html/rfc7636), pronounced pixie) by default.
- Bearer tokens should not be sent as a URL query parameter.
- Refresh tokens must be single use unless otherwise tied to the sender. This application will issue single-use refresh tokens.