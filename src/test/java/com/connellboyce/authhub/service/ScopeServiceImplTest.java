package com.connellboyce.authhub.service;

import com.connellboyce.authhub.exception.ResourceConflictException;
import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScopeServiceImplTest {
	@InjectMocks
	private ScopeServiceImpl scopeService;

	@Mock
	private ScopeRepository scopeRepository;

	@Mock
	private ApplicationService applicationService;

	@Mock
	private MongoRegisteredClientRepository clientRepository;

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


	private static final Application OWNED_APP = new Application("app123", "App", "Desc", "owner123");

	@Test
	void getScopesByNames_delegatesToRepository() {
		Scope scope = new Scope("scope1", "read", "app123");
		when(scopeRepository.findByNameIn(List.of("read"))).thenReturn(List.of(scope));

		assertEquals(List.of(scope), scopeService.getScopesByNames(List.of("read")));
	}

	@Test
	void getOwnedApplicationScopes_returnsScopesOrEmpty() {
		Scope scope = new Scope("scope1", "read", "app123");
		when(applicationService.getOwnedApplication("app123", "owner123")).thenReturn(OWNED_APP);
		when(scopeRepository.findByApplicationId("app123")).thenReturn(Optional.of(List.of(scope)));

		assertEquals(List.of(scope), scopeService.getOwnedApplicationScopes("app123", "owner123"));

		when(scopeRepository.findByApplicationId("app123")).thenReturn(Optional.empty());
		assertEquals(List.of(), scopeService.getOwnedApplicationScopes("app123", "owner123"));
	}

	@Test
	void getOwnedApplicationScopes_notOwned_throwsNotFound() {
		when(applicationService.getOwnedApplication("app123", "intruder")).thenThrow(new ResourceNotFoundException("Application not found"));

		assertThrows(ResourceNotFoundException.class, () -> scopeService.getOwnedApplicationScopes("app123", "intruder"));
	}

	@Test
	void createOwnedScope_success() {
		when(applicationService.getOwnedApplication("app123", "owner123")).thenReturn(OWNED_APP);
		when(scopeRepository.findByName("read")).thenReturn(Optional.empty());
		when(scopeRepository.save(any(Scope.class))).thenAnswer(i -> i.getArgument(0));

		Scope created = scopeService.createOwnedScope("app123", "read", "owner123");

		assertNotNull(created.getId());
		assertEquals("read", created.getName());
		assertEquals("app123", created.getApplicationId());
	}

	@Test
	void createOwnedScope_blankName_throwsIllegalArgument() {
		when(applicationService.getOwnedApplication("app123", "owner123")).thenReturn(OWNED_APP);

		assertThrows(IllegalArgumentException.class, () -> scopeService.createOwnedScope("app123", " ", "owner123"));
		assertThrows(IllegalArgumentException.class, () -> scopeService.createOwnedScope("app123", null, "owner123"));
	}

	@Test
	void createOwnedScope_duplicateName_throwsConflict() {
		when(applicationService.getOwnedApplication("app123", "owner123")).thenReturn(OWNED_APP);
		when(scopeRepository.findByName("read")).thenReturn(Optional.of(new Scope("other", "read", "otherApp")));

		assertThrows(ResourceConflictException.class, () -> scopeService.createOwnedScope("app123", "read", "owner123"));
	}

	@Test
	void createOwnedScope_notOwned_throwsNotFound() {
		when(applicationService.getOwnedApplication("app123", "intruder")).thenThrow(new ResourceNotFoundException("Application not found"));

		assertThrows(ResourceNotFoundException.class, () -> scopeService.createOwnedScope("app123", "read", "intruder"));
		verify(scopeRepository, never()).save(any());
	}

	@Test
	void deleteOwnedScope_unassigned_deletes() {
		when(applicationService.getOwnedApplication("app123", "owner123")).thenReturn(OWNED_APP);
		when(scopeRepository.findByIdAndApplicationId("scope1", "app123")).thenReturn(Optional.of(new Scope("scope1", "read", "app123")));
		when(clientRepository.countByScopesContaining("read")).thenReturn(0L);

		scopeService.deleteOwnedScope("app123", "scope1", "owner123");

		verify(scopeRepository).deleteById("scope1");
	}

	@Test
	void deleteOwnedScope_assigned_throwsConflictWithCountOnly() {
		when(applicationService.getOwnedApplication("app123", "owner123")).thenReturn(OWNED_APP);
		when(scopeRepository.findByIdAndApplicationId("scope1", "app123")).thenReturn(Optional.of(new Scope("scope1", "read", "app123")));
		when(clientRepository.countByScopesContaining("read")).thenReturn(3L);

		ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> scopeService.deleteOwnedScope("app123", "scope1", "owner123"));

		assertEquals("Scope is assigned to 3 client(s) and cannot be deleted", ex.getMessage());
		verify(scopeRepository, never()).deleteById(any());
	}

	@Test
	void deleteOwnedScope_scopeOfOtherApplication_throwsNotFound() {
		when(applicationService.getOwnedApplication("app123", "owner123")).thenReturn(OWNED_APP);
		when(scopeRepository.findByIdAndApplicationId("scope1", "app123")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> scopeService.deleteOwnedScope("app123", "scope1", "owner123"));
		verify(scopeRepository, never()).deleteById(any());
	}

	@Test
	void deleteOwnedScope_notOwned_throwsNotFound() {
		when(applicationService.getOwnedApplication("app123", "intruder")).thenThrow(new ResourceNotFoundException("Application not found"));

		assertThrows(ResourceNotFoundException.class, () -> scopeService.deleteOwnedScope("app123", "scope1", "intruder"));
		verify(scopeRepository, never()).deleteById(any());
	}
}
