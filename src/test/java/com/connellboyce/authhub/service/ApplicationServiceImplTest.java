package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

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

	@InjectMocks
	private ApplicationServiceImpl applicationService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testCreateApplication_invalidInput() {
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication(null, "desc", "owner"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("name", "", "owner"));
		assertThrows(IllegalArgumentException.class, () -> applicationService.createApplication("", "desc", "owner"));
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
}