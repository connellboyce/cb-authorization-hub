# OAuth Server and Client Portal
### An OAuth 2.1 implementation with Spring Authorization Server and a Spring Web based portal to manage clients and scopes.

<p align="center">
    <img alt="JDK Version" src="https://img.shields.io/badge/JDK-21-CC5500"/>
    <img alt="Spring Boot Version" src="https://img.shields.io/badge/Spring Boot-3.3.3-green"/>
    <a href="/LICENSE"><img alt="License" src="https://img.shields.io/badge/license-CC0--1.0-blue.svg"/></a>
<br>
    <a href="https://github.com/connellboyce/cb-authorization-hub/actions/workflows/build.yml"><img alt="Actions Status" src="https://github.com/connellboyce/cb-authorization-hub/actions/workflows/build.yml/badge.svg"></a>
</p>

<hr>

### Table of Contents
  - [Overview](#overview)
    - [OAuth2.1 Authorization Server](#oauth21-authorization-server)
        - [OAuth Endpoints](#oauth-endpoints) 
        - [Supported Grants](#supported-grants)
        - [OpenID Connect Compliant](#openid-connect-compliant)
    - [Login Page](#login-page)
    - [Developer Hub](#developer-hub)
      - [Application Registration](#application-registration)
      - [OAuth Client Registration](#oauth-client-registration)
  - [References](#references)
    - [OAuth 2.1](#oauth-21)

## Overview

### OAuth2.1 Authorization Server

#### OAuth Endpoints
- **/oauth2/authorize**
  - Authorization endpoint
  - Redirects to the login page if not authenticated
  - Redirects to the client application after successful authentication
- **/oauth2/token**
  - Token endpoint
  - Exchanges authorization code for access token
  - Exchanges refresh token for new access token
- **/oauth2/jwks**
  - JSON Web Key Set endpoint
  - Provides public keys for verifying JWTs

#### Supported Grants
- Authorization Code
- Client Credentials
- Refresh Token
- On-Behalf-Of Token Exchange (Work in Progress)

#### OpenID Connect Compliant
- Supports OpenID Connect
- Provides ID Token
- Supports UserInfo endpoint (Work in Progress)

### Login Page
- Authentication
  - Username and password authentication
- Registration
  - User registration


### Developer Hub

#### Application Management
- Register a new application
- Update an application
- Delete an application
- Define scopes owned by the application

#### OAuth Client Management
- Register a new OAuth client
- Update an OAuth client
- Delete an OAuth client
- Define grants to be consumed by the OAuth client
- Define scopes to be consumed by the OAuth client

### Additional Features

#### Well Known Endpoints
- /.well-known/robots.txt
- /.well-known/humans.txt

#### Spring Actuator Endpoints
- /actuator/health
- /actuator/info

## References

### OAuth 2.1
The OAuth 2.1 Authorization Framework ([proposal](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-10)) is an extension of the OAuth 2.0 standard ([RFC6749](https://datatracker.ietf.org/doc/html/rfc6749)) authorization server.

### On-Behalf-Of Grant
Also known as token exchange grant, On-Behalf-Of grant [RFC8693](https://datatracker.ietf.org/doc/html/rfc8693) is an extension of OAuth2.0 and is used to exchange a token for another token on behalf of a user. This is useful for scenarios where a client application needs to access a resource on behalf of a user, but does not have the user's credentials. Microsoft has [one such implementation](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-on-behalf-of-flow)
