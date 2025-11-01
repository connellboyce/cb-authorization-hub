package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.AuthUtilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        Application created = new Application("1", "My App", "Test description", "user123");
        when(applicationService.createApplication(eq("My App"), eq("Test description"), eq("user123")))
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
    void testCreateApplication_insufficientRole() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        Application created = new Application("1", "My App", "Test description", "user123");
        when(applicationService.createApplication(eq("My App"), eq("Test description"), eq("user123")))
                .thenReturn(created);

        try {
            mockMvc.perform(post("/portal/operation/application")
                            .with(csrf())
                            .with(user("user123").roles("USER"))
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testCreateApplication_noCsrfToken() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
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
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
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

    @Test
    void testCreateApplication_failure() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));

        when(applicationService.createApplication(eq("My App"), eq("Test description"), eq("user123")))
                .thenReturn(null);

        try {
            mockMvc.perform(post("/portal/operation/application")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/applications"))
                    .andExpect(flash().attribute("error", "Application creation failed"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateApplication() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(true);

        Application updated = new Application("1", "My App", "Test description", "user123");
        when(applicationService.updateApplication(anyString(), eq("My App"), eq("Test description"), eq("user123")))
                .thenReturn(updated);

        try {
            mockMvc.perform(put("/portal/operation/application")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("id", "1")
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/applications/1"))
                    .andExpect(flash().attribute("success", "Application updated successfully!"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateApplication_insufficientRole() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(true);

        Application updated = new Application("1", "My App", "Test description", "user123");
        when(applicationService.updateApplication(anyString(), eq("My App"), eq("Test description"), eq("user123")))
                .thenReturn(updated);

        try {
            mockMvc.perform(put("/portal/operation/application")
                            .with(csrf())
                            .with(user("user123").roles("USER"))
                            .param("id", "1")
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateApplication_identityDoesNotOwnResource() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(false);

        Application updated = new Application("1", "My App", "Test description", "user123");
        when(applicationService.updateApplication(anyString(), eq("My App"), eq("Test description"), eq("user123")))
                .thenReturn(updated);

        try {
            mockMvc.perform(put("/portal/operation/application")
                            .with(csrf())
                            .with(user("user123").roles("DEVELOPER"))
                            .param("id", "1")
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateApplication_noCsrfToken() {
        try {
            mockMvc.perform(put("/portal/operation/application")
                            .with(user("user123").roles("DEVELOPER"))
                            .param("id", "1")
                            .param("applicationName", "My App")
                            .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateApplication_unauthenticated() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.empty());

        try {
            mockMvc.perform(put("/portal/operation/application")
                    .with(csrf())
                    .param("id", "1")
                    .param("applicationName", "My App")
                    .param("description", "Test description"))
                    .andExpect(status().is(401));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testUpdateApplication_failure() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(true);

        when(applicationService.updateApplication(anyString(), eq("My App"), eq("Test description"), eq("user123")))
                .thenReturn(null);

        try {
            mockMvc.perform(put("/portal/operation/application")
                    .with(csrf())
                    .with(user("user123").roles("DEVELOPER"))
                    .param("id", "1")
                    .param("applicationName", "My App")
                    .param("description", "Test description"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/applications/1"))
                    .andExpect(flash().attribute("error", "Application update failed"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testDeleteApplication() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(true);

        try {
            mockMvc.perform(delete("/portal/operation/application/1")
                    .with(csrf())
                    .with(user("user123").roles("DEVELOPER"))
                    .param("id", "1")
                    .param("applicationName", "My App")
                    .param("description", "Test description"))
                    .andExpect(status().is(302))
                    .andExpect(redirectedUrl("/portal/applications"))
                    .andExpect(flash().attribute("success", "Application deleted successfully!"));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testDeleteApplication_insufficientRole() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(true);

        try {
            mockMvc.perform(delete("/portal/operation/application/1")
                    .with(csrf())
                    .with(user("user123").roles("USER"))
                    .param("id", "1")
                    .param("applicationName", "My App")
                    .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testDeleteApplication_missingCsrfToken() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(true);

        try {
            mockMvc.perform(delete("/portal/operation/application/1")
                    .with(user("user123").roles("DEVELOPER"))
                    .param("id", "1")
                    .param("applicationName", "My App")
                    .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testDeleteApplication_identityDoesNotOwnResource() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.of("user123"));
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(false);

        try {
            mockMvc.perform(delete("/portal/operation/application/1")
                    .with(csrf())
                    .with(user("user123").roles("DEVELOPER"))
                    .param("id", "1")
                    .param("applicationName", "My App")
                    .param("description", "Test description"))
                    .andExpect(status().is(403));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

    @Test
    void testDeleteApplication_unauthenticated() {
        when(authUtilService.getUserIdFromAuthentication(any(Authentication.class)))
                .thenReturn(Optional.empty());
        when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
                .thenReturn(true);

        try {
            mockMvc.perform(delete("/portal/operation/application/1")
                    .with(csrf())
                    .param("id", "1")
                    .param("applicationName", "My App")
                    .param("description", "Test description"))
                    .andExpect(status().is(401));
        } catch (Exception e) {
            fail("Encountered exception when creating an application: " + e.getMessage());
        }
    }

}
