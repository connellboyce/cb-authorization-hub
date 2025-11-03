package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.repository.ScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScopeServiceImplTest {
	@InjectMocks
	private ScopeServiceImpl scopeService;

	@Mock
	private ScopeRepository scopeRepository;

	@Mock
	private ApplicationService applicationService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testCreateScope_success() {
		when(applicationService.getApplicationById("app123"))
				.thenReturn(new Application("app123", "Test App", "A test application", "owner123"));
		when(scopeRepository.save(any(Scope.class)))
				.thenAnswer(i -> i.getArgument(0));

		Scope scope = scopeService.createScope("urn:cb:scope:test", "app123");
		assertNotNull(scope);
		assertEquals("urn:cb:scope:test", scope.getName());
		assertEquals("app123", scope.getApplicationId());
		verify(scopeRepository).save(any(Scope.class));
	}

	@Test
	void testCreateScope_illegalInputs() {
		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope(null, "app123"));
		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope("name", null));
		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope(null, null));
		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope("", "app123"));
		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope("name", ""));
		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope("", ""));
	}

	@Test
	void testCreateScope_duplicateNameExists() {
		when(scopeRepository.findByName("urn:cb:scope:existing"))
				.thenReturn(Optional.of(new Scope("scope123", "urn:cb:scope:existing", "app123")));

		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope("urn:cb:scope:existing", "app123"));
	}

	@Test
	void testCreateScope_invalidApplicationId() {
		when(applicationService.getApplicationById("invalidAppId"))
				.thenReturn(null);

		assertThrows(IllegalArgumentException.class, () -> scopeService.createScope("urn:cb:scope:test", "invalidAppId"));
	}

	@Test
	void testGetScopesByApplicationId_success() {
		when(scopeRepository.findByApplicationId("app123"))
				.thenReturn(java.util.Optional.of(java.util.List.of(
						new Scope("scope1", "urn:cb:scope:one", "app123"),
						new Scope("scope2", "urn:cb:scope:two", "app123")
				)));

		List<Scope> scopes = scopeService.getScopesByApplicationId("app123");
		assertNotNull(scopes);
		assertEquals(2, scopes.size());
	}

	@Test
	void testGetScopesByApplicationId_invalidInput() {
		assertThrows(IllegalArgumentException.class, () -> scopeService.getScopesByApplicationId(null));
		assertThrows(IllegalArgumentException.class, () -> scopeService.getScopesByApplicationId(""));
	}

	@Test
	void testGetScopesByApplicationId_noneFound() {
		when(scopeRepository.findByApplicationId("app123"))
				.thenReturn(Optional.empty());

		java.util.List<Scope> scopes = scopeService.getScopesByApplicationId("app123");
		assertNull(scopes);
	}

	@Test
	void testGetAllScopes_success() {
		when(scopeRepository.findAll())
				.thenReturn(java.util.List.of(
						new Scope("scope1", "urn:cb:scope:one", "app123"),
						new Scope("scope2", "urn:cb:scope:two", "app456")
				));

		java.util.List<Scope> scopes = scopeService.getAllScopes();
		assertNotNull(scopes);
		assertEquals(2, scopes.size());
	}

}