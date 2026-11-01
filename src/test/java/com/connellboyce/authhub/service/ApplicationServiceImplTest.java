package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.exception.ResourceConflictException;
import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.repository.ApplicationRepository;
import com.connellboyce.authhub.repository.ScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationServiceImplTest {

	@Mock
	private ApplicationRepository applicationRepository;

	@Mock
	private UserService userService;

	@Mock
	private ScopeRepository scopeRepository;

	@InjectMocks
	private ApplicationServiceImpl applicationService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		AuthUtilServiceImpl authUtilService = new AuthUtilServiceImpl();
		ReflectionTestUtils.setField(authUtilService, "userService", userService);
		ReflectionTestUtils.setField(applicationService, "authUtilService", authUtilService);
	}

	@Test
	void testCreateApplication_invalidInput() {
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication(null, "desc", "owner"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("", "desc", "owner"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("name", "", "owner"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("name", null, "owner"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("name", "desc", ""));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("name", "desc", null));
	}

	@Test
	void createApplication_duplicateNameExists() {
		when(applicationRepository.findByName("existing")).thenReturn(Optional.of(new Application("1", "existing", "desc", "owner")));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("existing", "desc", "owner"));
	}

	@Test
	void createApplication_success() {
		when(applicationRepository.findByName("new")).thenReturn(Optional.empty());
		when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

		Application result = applicationService.createApplication("new", "desc", "owner123");

		assertNotNull(result);
		assertEquals("new", result.getName());
		assertEquals("desc", result.getDescription());
		assertEquals("owner123", result.getOwnerId());
		verify(applicationRepository).save(any(Application.class));
	}


	@Test
	void getApplicationById_success() {
		Application app = new Application("1", "App", "Desc", "owner");
		when(applicationRepository.findById("1")).thenReturn(Optional.of(app));

		Application result = applicationService.getApplicationById("1");

		assertEquals(app, result);
	}

	@Test
	void getApplicationById_notFound() {
		when(applicationRepository.findById("missing")).thenReturn(Optional.empty());
		assertNull(applicationService.getApplicationById("missing"));
	}

	@Test
	void getApplicationsByOwnerId_success() {
		List<Application> apps = List.of(new Application("1", "A", "D", "owner"));
		when(applicationRepository.findByOwnerId("owner")).thenReturn(Optional.of(apps));

		List<Application> result = applicationService.getApplicationsByOwnerId("owner");

		assertEquals(1, result.size());
	}

	@Test
	void getApplicationsByOwnerId_noneFound() {
		when(applicationRepository.findByOwnerId("owner")).thenReturn(Optional.empty());
		assertNull(applicationService.getApplicationsByOwnerId("owner"));
	}

	@Test
	void updateApplication_success() {
		Application expected = new Application("1", "Updated", "Desc", "owner");
		when(applicationRepository.save(any(Application.class))).thenReturn(expected);

		Application result = applicationService.updateApplication("1", "Updated", "Desc", "owner");

		assertEquals(expected, result);
		verify(applicationRepository).save(expected);
	}

	@Test
	void deleteApplicationById_success() {
		applicationService.deleteApplicationById("1");
		verify(applicationRepository).deleteApplicationById("1");
	}

	@Test
	void testvalidateApplicationOwnership_returnsFalse_noAuthentication() {
		assertFalse(applicationService.validateApplicationOwnership(null, "appId"));
	}

	@Test
	void validateApplicationOwnership_returnsFalse_whenAuthOrApplicationIdInvalid() {
		assertFalse(applicationService.validateApplicationOwnership(null, "id"));
		Authentication mockAuth = mock(Authentication.class);
		assertFalse(applicationService.validateApplicationOwnership(mockAuth, ""));
	}

	@Test
	void validateApplicationOwnership_returnsFalse_whenUserIsNotFound() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);
		when(userService.getCBUserByUsername("alice")).thenReturn(null);
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(new Application("app123", "A", "D", "owner123")));

		assertFalse(applicationService.validateApplicationOwnership(auth, "app123"));
	}

	@Test
	void validateApplicationOwnership_returnsFalse_whenApplicationIsNotFound() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);
		when(userService.getCBUserByUsername("alice")).thenReturn(new CBUser("1", "user", "pass", Set.of(), "email", "Alice", "L"));
		when(applicationRepository.findById("app123")).thenReturn(Optional.empty());

		assertFalse(applicationService.validateApplicationOwnership(auth, "app123"));
	}

	@Test
	void validateApplicationOwnership_returnsFalse_whenOwnerIdOfApplicationMissing() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);

		Application app = new Application("app123", "A", "D", "");
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(app));

		assertFalse(applicationService.validateApplicationOwnership(auth, "app123"));
	}

	@Test
	void validateApplicationOwnership_returnsTrue_whenOwnerMatches() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);

		CBUser user = new CBUser("owner123", "alice", "pass", Set.of(), "email", "Alice", "Smith");

		Application app = new Application("app123", "App", "Desc", "owner123");

		when(userService.getCBUserByUsername("alice")).thenReturn(user);
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(app));

		boolean result = applicationService.validateApplicationOwnership(auth, "app123");

		assertTrue(result);
	}

	@Test
	void validateApplicationOwnership_returnsFalse_whenOwnerIdEmpty() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);

		CBUser user = new CBUser("owner123", "alice", "pass", Set.of(), "email", "Alice", "Smith");

		Application app = new Application("app123", "App", "Desc", "");

		when(userService.getCBUserByUsername("alice")).thenReturn(user);
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(app));

		boolean result = applicationService.validateApplicationOwnership(auth, "app123");

		assertFalse(result);
	}

	@Test
	void validateApplicationOwnership_returnsFalse_whenOwnerIdNull() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);

		CBUser user = new CBUser("owner123", "alice", "pass", Set.of(), "email", "Alice", "Smith");

		Application app = new Application("app123", "App", "Desc", null);

		when(userService.getCBUserByUsername("alice")).thenReturn(user);
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(app));

		boolean result = applicationService.validateApplicationOwnership(auth, "app123");

		assertFalse(result);
	}

	@Test
	void testValidateApplicationOwnership_returnsFalse_whenApplicationIdNullOrEmpty() {
		Authentication auth = mock(Authentication.class);
		assertFalse(applicationService.validateApplicationOwnership(auth, null));
		assertFalse(applicationService.validateApplicationOwnership(auth, ""));
	}

	@Test
	void validateApplicationOwnership_jwtPrincipal_trueWhenSubMatchesOwner() {
		Authentication auth = mock(Authentication.class);
		when(auth.getPrincipal()).thenReturn(userJwt("owner123"));
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(new Application("app123", "App", "Desc", "owner123")));

		assertTrue(applicationService.validateApplicationOwnership(auth, "app123"));
	}

	@Test
	void validateApplicationOwnership_jwtPrincipal_falseWhenSubDiffersFromOwner() {
		Authentication auth = mock(Authentication.class);
		when(auth.getPrincipal()).thenReturn(userJwt("someone-else"));
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(new Application("app123", "App", "Desc", "owner123")));

		assertFalse(applicationService.validateApplicationOwnership(auth, "app123"));
	}

	@Test
	void validateApplicationOwnership_jwtPrincipal_falseForClientCredentialsToken() {
		Authentication auth = mock(Authentication.class);
		when(auth.getPrincipal()).thenReturn(Jwt.withTokenValue("token").header("alg", "none").subject("owner123").build());
		when(applicationRepository.findById("app123")).thenReturn(Optional.of(new Application("app123", "App", "Desc", "owner123")));

		assertFalse(applicationService.validateApplicationOwnership(auth, "app123"));
	}

	private Jwt userJwt(String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).claim("username", "alice").build();
	}

	@Test
	void getOwnedApplications_returnsOwnedOrEmpty() {
		Application app = new Application("app123", "App", "Desc", "owner123");
		when(applicationRepository.findByOwnerId("owner123")).thenReturn(Optional.of(List.of(app)));
		when(applicationRepository.findByOwnerId("nobody")).thenReturn(Optional.empty());

		assertEquals(List.of(app), applicationService.getOwnedApplications("owner123"));
		assertEquals(List.of(), applicationService.getOwnedApplications("nobody"));
	}

	@Test
	void getApplicationsByIds_delegatesToRepository() {
		Application app = new Application("app123", "App", "Desc", "owner123");
		when(applicationRepository.findAllById(List.of("app123"))).thenReturn(List.of(app));

		assertEquals(List.of(app), applicationService.getApplicationsByIds(List.of("app123")));
	}

	@Test
	void getOwnedApplication_foundForOwner() {
		Application app = new Application("app123", "App", "Desc", "owner123");
		when(applicationRepository.findByIdAndOwnerId("app123", "owner123")).thenReturn(Optional.of(app));

		assertEquals(app, applicationService.getOwnedApplication("app123", "owner123"));
	}

	@Test
	void getOwnedApplication_notOwned_throwsNotFound() {
		when(applicationRepository.findByIdAndOwnerId("app123", "intruder")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> applicationService.getOwnedApplication("app123", "intruder"));
	}

	@Test
	void createOwnedApplication_success() {
		when(applicationRepository.findByName("App")).thenReturn(Optional.empty());
		when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

		Application created = applicationService.createOwnedApplication("App", "Desc", "owner123");

		assertNotNull(created.getId());
		assertEquals("App", created.getName());
		assertEquals("Desc", created.getDescription());
		assertEquals("owner123", created.getOwnerId());
	}

	@Test
	void createOwnedApplication_blankFields_throwIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> applicationService.createOwnedApplication(" ", "Desc", "owner123"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createOwnedApplication(null, "Desc", "owner123"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createOwnedApplication("App", "", "owner123"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createOwnedApplication("App", null, "owner123"));
	}

	@Test
	void createOwnedApplication_duplicateName_throwsConflict() {
		when(applicationRepository.findByName("App")).thenReturn(Optional.of(new Application("other", "App", "Desc", "someone")));

		assertThrows(ResourceConflictException.class, () -> applicationService.createOwnedApplication("App", "Desc", "owner123"));
		verify(applicationRepository, never()).save(any());
	}

	@Test
	void updateOwnedApplication_success_keepsOwner() {
		when(applicationRepository.findByIdAndOwnerId("app123", "owner123")).thenReturn(Optional.of(new Application("app123", "Old", "Old desc", "owner123")));
		when(applicationRepository.findByName("New")).thenReturn(Optional.empty());
		when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

		Application updated = applicationService.updateOwnedApplication("app123", "New", "New desc", "owner123");

		assertEquals("New", updated.getName());
		assertEquals("New desc", updated.getDescription());
		assertEquals("owner123", updated.getOwnerId());
	}

	@Test
	void updateOwnedApplication_sameName_succeeds() {
		Application app = new Application("app123", "App", "Old desc", "owner123");
		when(applicationRepository.findByIdAndOwnerId("app123", "owner123")).thenReturn(Optional.of(app));
		when(applicationRepository.findByName("App")).thenReturn(Optional.of(app));
		when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

		assertEquals("New desc", applicationService.updateOwnedApplication("app123", "App", "New desc", "owner123").getDescription());
	}

	@Test
	void updateOwnedApplication_nameTakenByOther_throwsConflict() {
		when(applicationRepository.findByIdAndOwnerId("app123", "owner123")).thenReturn(Optional.of(new Application("app123", "App", "Desc", "owner123")));
		when(applicationRepository.findByName("Taken")).thenReturn(Optional.of(new Application("other", "Taken", "Desc", "someone")));

		assertThrows(ResourceConflictException.class, () -> applicationService.updateOwnedApplication("app123", "Taken", "Desc", "owner123"));
		verify(applicationRepository, never()).save(any());
	}

	@Test
	void updateOwnedApplication_notOwned_throwsNotFound() {
		when(applicationRepository.findByIdAndOwnerId("app123", "intruder")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> applicationService.updateOwnedApplication("app123", "App", "Desc", "intruder"));
	}

	@Test
	void updateOwnedApplication_blankName_throwsIllegalArgument() {
		when(applicationRepository.findByIdAndOwnerId("app123", "owner123")).thenReturn(Optional.of(new Application("app123", "App", "Desc", "owner123")));

		assertThrows(IllegalArgumentException.class, () -> applicationService.updateOwnedApplication("app123", "", "Desc", "owner123"));
	}

	@Test
	void deleteOwnedApplication_withoutScopes_deletes() {
		when(applicationRepository.findByIdAndOwnerId("app123", "owner123")).thenReturn(Optional.of(new Application("app123", "App", "Desc", "owner123")));
		when(scopeRepository.existsByApplicationId("app123")).thenReturn(false);

		applicationService.deleteOwnedApplication("app123", "owner123");

		verify(applicationRepository).deleteById("app123");
	}

	@Test
	void deleteOwnedApplication_withScopes_throwsConflict() {
		when(applicationRepository.findByIdAndOwnerId("app123", "owner123")).thenReturn(Optional.of(new Application("app123", "App", "Desc", "owner123")));
		when(scopeRepository.existsByApplicationId("app123")).thenReturn(true);

		assertThrows(ResourceConflictException.class, () -> applicationService.deleteOwnedApplication("app123", "owner123"));
		verify(applicationRepository, never()).deleteById(any());
	}

	@Test
	void deleteOwnedApplication_notOwned_throwsNotFound() {
		when(applicationRepository.findByIdAndOwnerId("app123", "intruder")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> applicationService.deleteOwnedApplication("app123", "intruder"));
		verify(applicationRepository, never()).deleteById(any());
	}
}
