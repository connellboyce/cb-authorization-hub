package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.repository.ApplicationRepository;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import com.connellboyce.authhub.repository.ScopeRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Set;

import static com.connellboyce.authhub.support.TestTokens.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ScopeApiControllerTest {
	private static final String OWNER = "owner-1";
	private static final String INTRUDER = "intruder-1";
	private static final Application OWNED_APP = new Application("app-1", "App", "Desc", OWNER);

	@Autowired
	MockMvc mockMvc;

	@MockBean
	ApplicationRepository applicationRepository;

	@MockBean
	ScopeRepository scopeRepository;

	@MockBean
	MongoRegisteredClientRepository clientRepository;

	@BeforeEach
	void setup() {
		when(applicationRepository.findByIdAndOwnerId("app-1", OWNER)).thenReturn(Optional.of(OWNED_APP));
		when(applicationRepository.findByIdAndOwnerId("app-1", INTRUDER)).thenReturn(Optional.empty());
	}

	@Test
	void listScopes_owner_returnsApplicationScopes() throws Exception {
		when(scopeRepository.findByApplicationId("app-1")).thenReturn(Optional.of(List.of(new Scope("scope-1", "urn:test#read", "app-1"))));

		mockMvc.perform(get("/api/v1/applications/app-1/scopes").with(developer(OWNER, APPLICATIONS_READ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scopes", hasSize(1)))
				.andExpect(jsonPath("$.scopes[0].id").value("scope-1"))
				.andExpect(jsonPath("$.scopes[0].name").value("urn:test#read"))
				.andExpect(jsonPath("$.scopes[0].application_id").value("app-1"));
	}

	@Test
	void listScopes_notOwned_returns404() throws Exception {
		mockMvc.perform(get("/api/v1/applications/app-1/scopes").with(developer(INTRUDER, APPLICATIONS_READ)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void createScope_returns201WithLocation() throws Exception {
		when(scopeRepository.findByName("urn:test#write")).thenReturn(Optional.empty());
		when(scopeRepository.save(any(Scope.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(post("/api/v1/applications/app-1/scopes").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"urn:test#write\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("http://localhost/api/v1/applications/app-1/scopes/[0-9a-f-]+")))
				.andExpect(jsonPath("$.name").value("urn:test#write"))
				.andExpect(jsonPath("$.application_id").value("app-1"));
	}

	@Test
	void createScope_blankName_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/applications/app-1/scopes").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Scope name is required"));
	}

	@Test
	void createScope_duplicateName_returns409() throws Exception {
		when(scopeRepository.findByName("urn:test#write")).thenReturn(Optional.of(new Scope("other", "urn:test#write", "app-2")));

		mockMvc.perform(post("/api/v1/applications/app-1/scopes").with(developer(OWNER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"urn:test#write\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").exists())
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("Scope name 'urn:test#write' already exists"));
	}

	@Test
	void createScope_notOwnedApplication_returns404() throws Exception {
		mockMvc.perform(post("/api/v1/applications/app-1/scopes").with(developer(INTRUDER, APPLICATIONS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"urn:test#write\"}"))
				.andExpect(status().isNotFound());

		verify(scopeRepository, never()).save(any());
	}

	@Test
	void deleteScope_belongingToOtherApplication_returns404() throws Exception {
		when(scopeRepository.findByIdAndApplicationId("scope-9", "app-1")).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/v1/applications/app-1/scopes/scope-9").with(developer(OWNER, APPLICATIONS_WRITE)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

		verify(scopeRepository, never()).deleteById(any());
	}

	@Test
	void deleteScope_assigned_returns409WithCountOnly() throws Exception {
		when(scopeRepository.findByIdAndApplicationId("scope-1", "app-1")).thenReturn(Optional.of(new Scope("scope-1", "urn:test#read", "app-1")));
		when(clientRepository.countByScopesContaining("urn:test#read")).thenReturn(3L);

		mockMvc.perform(delete("/api/v1/applications/app-1/scopes/scope-1").with(developer(OWNER, APPLICATIONS_WRITE)))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Scope is assigned to 3 client(s) and cannot be deleted"));

		verify(scopeRepository, never()).deleteById(any());
	}

	@Test
	void deleteScope_unassigned_returns204() throws Exception {
		when(scopeRepository.findByIdAndApplicationId("scope-1", "app-1")).thenReturn(Optional.of(new Scope("scope-1", "urn:test#read", "app-1")));
		when(clientRepository.countByScopesContaining("urn:test#read")).thenReturn(0L);

		mockMvc.perform(delete("/api/v1/applications/app-1/scopes/scope-1").with(developer(OWNER, APPLICATIONS_WRITE)))
				.andExpect(status().isNoContent());

		verify(scopeRepository).deleteById("scope-1");
	}

	@Test
	void deleteScope_notOwnedApplication_returns404() throws Exception {
		mockMvc.perform(delete("/api/v1/applications/app-1/scopes/scope-1").with(developer(INTRUDER, APPLICATIONS_WRITE)))
				.andExpect(status().isNotFound());

		verify(scopeRepository, never()).deleteById(any());
	}

	@Test
	void scopeCatalog_listsAllOwnersScopesWithoutOwnerData() throws Exception {
		when(scopeRepository.findAll()).thenReturn(List.of(
				new Scope("scope-1", "urn:mine#read", "app-1"),
				new Scope("scope-2", "urn:theirs#read", "app-2")));
		when(applicationRepository.findAllById(Set.of("app-1", "app-2"))).thenReturn(List.of(
				OWNED_APP,
				new Application("app-2", "Their App", "Secret description", "someone-else")));

		mockMvc.perform(get("/api/v1/scopes").with(developer(OWNER, SCOPES_READ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scopes", hasSize(2)))
				.andExpect(jsonPath("$.scopes[*].name", containsInAnyOrder("urn:mine#read", "urn:theirs#read")))
				.andExpect(jsonPath("$.scopes[?(@.name == 'urn:theirs#read')].application.id").value("app-2"))
				.andExpect(jsonPath("$.scopes[?(@.name == 'urn:theirs#read')].application.name").value("Their App"))
				.andExpect(content().string(not(containsString("someone-else"))))
				.andExpect(content().string(not(containsString("Secret description"))));
	}

	@Test
	void scopeCatalog_skipsScopesWithoutApplication() throws Exception {
		when(scopeRepository.findAll()).thenReturn(List.of(new Scope("scope-1", "urn:orphan#read", "deleted-app")));
		when(applicationRepository.findAllById(Set.of("deleted-app"))).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/scopes").with(developer(OWNER, SCOPES_READ)))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"scopes\": []}", true));
	}
}
