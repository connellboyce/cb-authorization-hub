package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.repository.ApplicationRepository;
import com.connellboyce.authhub.repository.ScopeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.connellboyce.authhub.support.TestTokens.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApplicationApiControllerTest {
	private static final String OWNER = "owner-1";
	private static final String INTRUDER = "intruder-1";

	@Autowired
	MockMvc mockMvc;

	@MockBean
	ApplicationRepository applicationRepository;

	@MockBean
	ScopeRepository scopeRepository;

	@Test
	void listApplications_returnsOnlyOwned() throws Exception {
		when(applicationRepository.findByOwnerId(OWNER)).thenReturn(Optional.of(List.of(new Application("app-1", "App", "Desc", OWNER))));

		mockMvc.perform(get("/api/v1/applications").with(developer(OWNER, APPLICATIONS_READ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.applications", hasSize(1)))
				.andExpect(jsonPath("$.applications[0].id").value("app-1"))
				.andExpect(jsonPath("$.applications[0].ownerId").doesNotExist());

		verify(applicationRepository).findByOwnerId(OWNER);
		verify(applicationRepository, never()).findAll();
	}

	@Test
	void listApplications_noneOwned_returnsEmptyArray() throws Exception {
		when(applicationRepository.findByOwnerId(OWNER)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/applications").with(developer(OWNER, APPLICATIONS_READ)))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"applications\": []}", true));
	}

	@Test
	void createApplication_returns201WithLocation() throws Exception {
		when(applicationRepository.findByName("App")).thenReturn(Optional.empty());
		when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(post("/api/v1/applications").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"App\", \"description\": \"Desc\", \"owner_id\": \"someone-else\", \"ownerId\": \"someone-else\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("http://localhost/api/v1/applications/[0-9a-f-]+")))
				.andExpect(jsonPath("$.name").value("App"))
				.andExpect(jsonPath("$.description").value("Desc"));

		verify(applicationRepository).save(argThat(app -> OWNER.equals(app.getOwnerId())));
	}

	@Test
	void createApplication_blankField_returns400ProblemDetail() throws Exception {
		mockMvc.perform(post("/api/v1/applications").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"\", \"description\": \"Desc\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").exists())
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Application name is required"));
	}

	@Test
	void createApplication_malformedBody_returns400ProblemDetail() throws Exception {
		mockMvc.perform(post("/api/v1/applications").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void createApplication_duplicateName_returns409ProblemDetail() throws Exception {
		when(applicationRepository.findByName("App")).thenReturn(Optional.of(new Application("other", "App", "Desc", "someone")));

		mockMvc.perform(post("/api/v1/applications").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"App\", \"description\": \"Desc\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("Application name 'App' already exists"));
	}

	@Test
	void getApplication_owner_returns200() throws Exception {
		when(applicationRepository.findByIdAndOwnerId("app-1", OWNER)).thenReturn(Optional.of(new Application("app-1", "App", "Desc", OWNER)));

		mockMvc.perform(get("/api/v1/applications/app-1").with(developer(OWNER, APPLICATIONS_READ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("app-1"))
				.andExpect(jsonPath("$.name").value("App"));
	}

	@Test
	void getApplication_notOwned_isIndistinguishableFromMissing() throws Exception {
		when(applicationRepository.findByIdAndOwnerId(any(), any())).thenReturn(Optional.empty());

		String notOwned = mockMvc.perform(get("/api/v1/applications/app-1").with(developer(INTRUDER, APPLICATIONS_READ)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andReturn().getResponse().getContentAsString();
		String missing = mockMvc.perform(get("/api/v1/applications/missing").with(developer(INTRUDER, APPLICATIONS_READ)))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

		assertEquals(stripInstance(missing), stripInstance(notOwned));
	}

	@Test
	void updateApplication_owner_returns200() throws Exception {
		when(applicationRepository.findByIdAndOwnerId("app-1", OWNER)).thenReturn(Optional.of(new Application("app-1", "App", "Desc", OWNER)));
		when(applicationRepository.findByName("Renamed")).thenReturn(Optional.empty());
		when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(put("/api/v1/applications/app-1").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Renamed\", \"description\": \"New desc\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed"))
				.andExpect(jsonPath("$.description").value("New desc"));

		verify(applicationRepository).save(argThat(app -> OWNER.equals(app.getOwnerId())));
	}

	@Test
	void updateApplication_duplicateName_returns409() throws Exception {
		when(applicationRepository.findByIdAndOwnerId("app-1", OWNER)).thenReturn(Optional.of(new Application("app-1", "App", "Desc", OWNER)));
		when(applicationRepository.findByName("Taken")).thenReturn(Optional.of(new Application("app-2", "Taken", "Desc", "someone")));

		mockMvc.perform(put("/api/v1/applications/app-1").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Taken\", \"description\": \"Desc\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void updateApplication_notOwned_returns404() throws Exception {
		when(applicationRepository.findByIdAndOwnerId("app-1", INTRUDER)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/v1/applications/app-1").with(developer(INTRUDER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Hijacked\", \"description\": \"Desc\"}"))
				.andExpect(status().isNotFound());

		verify(applicationRepository, never()).save(any());
	}

	@Test
	void deleteApplication_withScopes_returns409() throws Exception {
		when(applicationRepository.findByIdAndOwnerId("app-1", OWNER)).thenReturn(Optional.of(new Application("app-1", "App", "Desc", OWNER)));
		when(scopeRepository.existsByApplicationId("app-1")).thenReturn(true);

		mockMvc.perform(delete("/api/v1/applications/app-1").with(developer(OWNER, APPLICATIONS_WRITE)))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Application has scopes and cannot be deleted"));

		verify(applicationRepository, never()).deleteById(any());
	}

	@Test
	void deleteApplication_withoutScopes_returns204() throws Exception {
		when(applicationRepository.findByIdAndOwnerId("app-1", OWNER)).thenReturn(Optional.of(new Application("app-1", "App", "Desc", OWNER)));
		when(scopeRepository.existsByApplicationId("app-1")).thenReturn(false);

		mockMvc.perform(delete("/api/v1/applications/app-1").with(developer(OWNER, APPLICATIONS_WRITE)))
				.andExpect(status().isNoContent());

		verify(applicationRepository).deleteById("app-1");
	}

	@Test
	void deleteApplication_notOwned_returns404() throws Exception {
		when(applicationRepository.findByIdAndOwnerId("app-1", INTRUDER)).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/v1/applications/app-1").with(developer(INTRUDER, APPLICATIONS_WRITE)))
				.andExpect(status().isNotFound());

		verify(applicationRepository, never()).deleteById(any());
	}

	private String stripInstance(String problemJson) {
		return problemJson.replaceAll("\"instance\":\"[^\"]*\",?", "");
	}
}
