package com.connellboyce.authhub;

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
public class JwksEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testJwks() {
        try {
            mockMvc.perform(get("/oauth2/jwks"))
                    .andExpect(status().is(200))
                    .andExpect(jsonPath("$.keys").isArray())
                    .andExpect(jsonPath("$.keys[0].kty").exists())
                    .andExpect(jsonPath("$.keys[0].e").exists())
                    .andExpect(jsonPath("$.keys[0].kid").exists())
                    .andExpect(jsonPath("$.keys[0].n").exists());
        } catch (Exception e) {
            fail("Exception occurred while attempting to get jwks: " + e.getMessage());
        }
    }
}
