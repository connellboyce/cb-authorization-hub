package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.AuthUtilService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApplicationsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ApplicationService applicationService;

    @MockBean
    AuthUtilService authUtilService;

    @MockBean
    Authentication authentication;

    @Test
    void testCreateApplication() {
        Mockito.when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        Application created = new Application("1", "My App", "Test description", "user123");
        Mockito.when(applicationService.createApplication(eq("My App"), eq("Test description"), eq("user123")))
                .thenReturn(created);

        try {
            mockMvc.perform(post("/portal/operation/application")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/applications"))
                    .andExpect(flash().attribute("success", "Application created successfully!"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testCreateApplication_noCsrf() {
        Mockito.when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        try {
            mockMvc.perform(post("/portal/operation/application")
                            .with(user("user123").roles("DEVELOPER"))
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testCreateApplication_unauthenticated() {
        Mockito.when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.empty());

        try {
            mockMvc.perform(post("/portal/operation/application")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/applications"))
                    .andExpect(flash().attribute("error", "User not authenticated"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

}
