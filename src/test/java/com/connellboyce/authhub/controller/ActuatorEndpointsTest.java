package com.connellboyce.authhub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetInfo() {
        try {
            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().is(200))
                    .andExpect(jsonPath("$.app.name").value("CB Authorization Hub"))
                    .andExpect(jsonPath("$.app.version").isString())
                    .andExpect(jsonPath("$.app.description").isString())
                    .andExpect(jsonPath("$.git.branch").isString())
                    .andExpect(jsonPath("$.git.commit.id").isString())
                    .andExpect(jsonPath("$.git.commit.time").isString());
        } catch (Exception e) {
            fail("Exception occurred while attempting to get jwks: " + e.getMessage());
        }
    }
}
