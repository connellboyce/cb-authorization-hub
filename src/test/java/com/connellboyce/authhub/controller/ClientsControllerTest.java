package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.service.AuthUtilService;
import com.connellboyce.authhub.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ClientsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientService clientService;

    @MockBean
    AuthUtilService authUtilService;

    @MockBean
    Authentication authentication;

    @Test
    void testCreateClient() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        MongoRegisteredClient created = new MongoRegisteredClient();
        when(clientService.createClient(any(RegisteredClient.class), eq("user123")))
                .thenReturn(created);

        try {
            mockMvc.perform(post("/portal/operation/client")
                    .with(csrf())
                    .with(user("user123").roles("DEVELOPER"))
                    .param("clientId", "client")
                    .param("clientSecret", "secret")
                    .param("grantTypes", "authorization_code")
                    .param("redirectUrls", "http://localhost:8080/callback")
                    .param("scopes", "openid"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/clients"))
                    .andExpect(flash().attribute("success", "Client created successfully!"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testCreateClient_insufficientRole() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        MongoRegisteredClient created = new MongoRegisteredClient();
        when(clientService.createClient(any(RegisteredClient.class), eq("user123")))
                .thenReturn(created);

        try {
            mockMvc.perform(post("/portal/operation/client")
                            .with(csrf())
                            .with(user("user123").roles("USER"))
                            .param("clientId", "client")
                            .param("clientSecret", "secret")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testCreateClient_missingCsrfToken() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        MongoRegisteredClient created = new MongoRegisteredClient();
        when(clientService.createClient(any(RegisteredClient.class), eq("user123")))
                .thenReturn(created);

        try {
            mockMvc.perform(post("/portal/operation/client")
                            .with(user("user123").roles("DEVELOPER"))
                            .param("clientId", "client")
                            .param("clientSecret", "secret")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testCreateClient_unauthenticated() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.empty());

        MongoRegisteredClient created = new MongoRegisteredClient();
        when(clientService.createClient(any(RegisteredClient.class), eq("user123")))
                .thenReturn(created);

        try {
            mockMvc.perform(post("/portal/operation/client")
                            .with(csrf())
                            .param("clientId", "client")
                            .param("clientSecret", "secret")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(401));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testCreateClient_failure() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        when(clientService.createClient(any(RegisteredClient.class), eq("user123")))
                .thenReturn(null);

        try {
            mockMvc.perform(post("/portal/operation/client")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("clientId", "client")
                            .param("clientSecret", "secret")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/clients"))
                    .andExpect(flash().attribute("error", "Failed to create client"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateClient() {
        try {
            when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                    .thenReturn(Optional.of("user123"));

            MongoRegisteredClient updated = new MongoRegisteredClient();
            when(clientService.updateClient(eq("client)"), any(), any(), any()))
                    .thenReturn(updated);

            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);


            mockMvc.perform(put("/portal/operation/client")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("clientId", "client")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/clients"))
                    .andExpect(flash().attribute("success", "Client updated successfully!"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateClient_insufficientRole() {
        try {
            when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                    .thenReturn(Optional.of("user123"));

            MongoRegisteredClient updated = new MongoRegisteredClient();
            when(clientService.updateClient(eq("client)"), any(), any(), any()))
                    .thenReturn(updated);

            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);

            mockMvc.perform(put("/portal/operation/client")
                            .with(csrf())
                            .with(user("user123").roles("USER"))
                            .param("clientId", "client")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateClient_missingCsrfToken() {
        try {
            when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                    .thenReturn(Optional.of("user123"));

            MongoRegisteredClient updated = new MongoRegisteredClient();
            when(clientService.updateClient(eq("client)"), any(), any(), any()))
                    .thenReturn(updated);

            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);


            mockMvc.perform(put("/portal/operation/client")
                            .with(user("user123").roles("DEVELOPER"))
                            .param("clientId", "client")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateClient_unauthenticated() {
        try {
            when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                    .thenReturn(Optional.empty());

            MongoRegisteredClient updated = new MongoRegisteredClient();
            when(clientService.updateClient(eq("client)"), any(), any(), any()))
                    .thenReturn(updated);

            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);

            mockMvc.perform(put("/portal/operation/client")
                            .with(csrf())
                            .param("clientId", "client")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(401));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateClient_identityDoesNotOwnResource() {
        try {
            when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                    .thenReturn(Optional.of("user123"));

            MongoRegisteredClient updated = new MongoRegisteredClient();
            when(clientService.updateClient(eq("client)"), any(), any(), any()))
                    .thenReturn(updated);

            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(false);

            mockMvc.perform(put("/portal/operation/client")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("clientId", "client")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateClient_updateFailed() {
        try {
            when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                    .thenReturn(Optional.of("user123"));

            when(clientService.updateClient(eq("client"), any(), any(), any()))
                    .thenThrow(new Exception("Client not found"));

            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);

            mockMvc.perform(put("/portal/operation/client")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("clientId", "client")
                            .param("grantTypes", "authorization_code")
                            .param("redirectUrls", "http://localhost:8080/callback")
                            .param("scopes", "openid"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/clients"))
                    .andExpect(flash().attribute("error", "Failed to update client"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testDeleteClient_success() {
        try {
            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);

            mockMvc.perform(delete("/portal/operation/client/{clientId}", "client")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER")))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/clients"))
                    .andExpect(flash().attribute("success", "Client deleted successfully!"));
        } catch (Exception e) {
            fail("Encountered exception when deleting a client: " + e.getMessage());
        }
    }

    @Test
    void testDeleteClient_insufficientRole() {
        try {
            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);

            mockMvc.perform(delete("/portal/operation/client/{clientId}", "client")
                            .with(csrf())
                            .with(user("user123").roles("USER")))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when deleting a client: " + e.getMessage());
        }
    }

    @Test
    void testDeleteClient_missingCsrfToken() {
        try {
            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);

            mockMvc.perform(delete("/portal/operation/client/{clientId}", "client")
                            .with(user("user123").roles("DEVELOPER")))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when deleting a client: " + e.getMessage());
        }
    }

    @Test
    void testDeleteClient_identityDoesNotOwnResource() {
        try {
            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(false);

            mockMvc.perform(delete("/portal/operation/client/{clientId}", "client")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER")))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when deleting a client: " + e.getMessage());
        }
    }

    @Test
    void testDeleteClient_unauthenticated() {
        try {
            when(clientService.validateClientOwnership(any(Authentication.class), eq("client")))
                    .thenReturn(true);

            mockMvc.perform(delete("/portal/operation/client/{clientId}", "client")
                            .with(csrf()))
                    .andExpect(status().is(401));
        } catch (Exception e) {
            fail("Encountered exception when deleting a client: " + e.getMessage());
        }
    }
}
