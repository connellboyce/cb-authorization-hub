package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
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
}
