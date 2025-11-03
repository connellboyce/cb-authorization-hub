package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.ClientService;
import com.connellboyce.authhub.service.ScopeService;
import com.connellboyce.authhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ClientService clientService;

	@MockBean
	private UserService userService;

	@MockBean
	private ApplicationService applicationService;

	@MockBean
	private ScopeService scopeService;


	@Test
	void testLoginAddsPreservedParamsAndClient_unauthenticated() throws Exception {
		when(clientService.getClientByClientId("abc123")).thenReturn(new MongoRegisteredClient());

		mockMvc.perform(get("/login")
						.sessionAttr("auth_param_client_id", "abc123"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("client_id", "abc123"))
				.andExpect(model().attributeExists("client"))
				.andExpect(view().name("login"));
	}

	@Test
	void testRegisterPage() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(view().name("register"));
	}

	@Test
	void testPortalHomePage_authenticated() throws Exception {
		mockMvc.perform(get("/portal/index")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("name", "testuser"))
				.andExpect(view().name("portal/index"));
	}

	@Test
	void testPortalHomePage_unauthenticated() throws Exception {
		mockMvc.perform(get("/portal/index"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testPortalHomePage_insufficientRole() throws Exception {
		mockMvc.perform(get("/portal/index")
						.with(user("testuser").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void testPortalClientsPage_authenticated() throws Exception {
		when(userService.getCBUserByUsername("testuser")).thenReturn(
				new CBUser("1",
						"testuser",
						"testpassword",
						Set.of("DEVELOPER"),
						"test-cbauth@mailinator.com",
						"Goopy",
						"Scoopy"));

		mockMvc.perform(get("/portal/clients")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(view().name("portal/clients"));
	}

	@Test
	void testPortalClientsPage_unauthenticated() throws Exception {
		mockMvc.perform(get("/portal/clients"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testPortalClientsPage_insufficientRole() throws Exception {
		mockMvc.perform(get("/portal/clients")
						.with(user("testuser").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void testPortalApplicationsPage_authenticated_noApps() throws Exception {
		when(userService.getCBUserByUsername("testuser")).thenReturn(
				new CBUser("1",
						"testuser",
						"testpassword",
						Set.of("DEVELOPER"),
						"test-cbauth@mailinator.com",
						"Goopy",
						"Scoopy"));
		mockMvc.perform(get("/portal/applications")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(view().name("portal/applications"));
	}

	@Test
	void testPortalApplicationsPage_unauthenticated() throws Exception {
		mockMvc.perform(get("/portal/applications"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testPortalApplicationsPage_insufficientRole() throws Exception {
		mockMvc.perform(get("/portal/applications")
						.with(user("testuser").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void testCreateApplicationPage_authenticated() throws Exception {
		mockMvc.perform(get("/portal/applications/create")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(view().name("portal/create-application"));
	}

	@Test
	void testCreateApplicationPage_unauthenticated() throws Exception {
		mockMvc.perform(get("/portal/applications/create"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testCreateApplicationPage_insufficientRole() throws Exception {
		mockMvc.perform(get("/portal/applications/create")
						.with(user("testuser").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void testCreateClientPage_authenticated_oneApplication_oneScope() throws Exception {
		Scope scope = new Scope("1", "urn:cb:scope:test", "1");

		Application app = new Application("1", "App One", "A test application", "owner1");

		when(scopeService.getAllScopes()).thenReturn(List.of(scope));
		when(applicationService.getApplicationById("1")).thenReturn(app);

		mockMvc.perform(get("/portal/clients/create")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("generatedSecret"))
				.andExpect(model().attributeExists("grantTypes"))
				.andExpect(model().attribute("scopesByApplication", aMapWithSize(1)))
				.andExpect(model().attribute("scopesByApplication", hasEntry(equalTo("App One"), hasItem(scope))))
				.andExpect(view().name("portal/create-client"));
	}

	@Test
	void testCreateClientPage_authenticated_oneApplication_multipleScopes() throws Exception {
		Scope scope1 = new Scope("1", "urn:cb:scope:test", "1");
		Scope scope2 = new Scope("2", "urn:cb:scope:test", "1");

		Application app = new Application("1", "App One", "A test application", "owner1");

		when(scopeService.getAllScopes()).thenReturn(List.of(scope1, scope2));
		when(applicationService.getApplicationById("1")).thenReturn(app);

		mockMvc.perform(get("/portal/clients/create")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("generatedSecret"))
				.andExpect(model().attributeExists("grantTypes"))
				.andExpect(model().attribute("scopesByApplication", aMapWithSize(1)))
				.andExpect(model().attribute("scopesByApplication", hasEntry(equalTo("App One"), hasItems(scope1, scope2))))
				.andExpect(view().name("portal/create-client"));
	}

	@Test
	void testCreateClientPage_authenticated_multipleApplications_oneScopeEach() throws Exception {
		Scope scope1 = new Scope("1", "urn:cb:scope:test", "1");
		Scope scope2 = new Scope("2", "urn:cb:scope:test", "2");

		Application app1 = new Application("1", "App One", "A test application", "owner1");
		Application app2 = new Application("2", "App Two", "A test application", "owner1");

		when(scopeService.getAllScopes()).thenReturn(List.of(scope1, scope2));
		when(applicationService.getApplicationById("1")).thenReturn(app1);
		when(applicationService.getApplicationById("2")).thenReturn(app2);

		mockMvc.perform(get("/portal/clients/create")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("generatedSecret"))
				.andExpect(model().attributeExists("grantTypes"))
				.andExpect(model().attribute("scopesByApplication", aMapWithSize(2)))
				.andExpect(model().attribute("scopesByApplication", hasEntry(equalTo("App One"), hasItem(scope1))))
				.andExpect(model().attribute("scopesByApplication", hasEntry(equalTo("App Two"), hasItem(scope2))))
				.andExpect(view().name("portal/create-client"));
	}

	@Test
	void testCreateClientPage_authenticated_multipleApplications_multipleScopesEach() throws Exception {
		Scope scope1 = new Scope("1", "urn:cb:scope:test", "1");
		Scope scope2 = new Scope("2", "urn:cb:scope:test", "1");
		Scope scope3 = new Scope("3", "urn:cb:scope:test", "2");
		Scope scope4 = new Scope("4", "urn:cb:scope:test", "2");

		Application app1 = new Application("1", "App One", "A test application", "owner1");
		Application app2 = new Application("2", "App Two", "A test application", "owner1");

		when(scopeService.getAllScopes()).thenReturn(List.of(scope1, scope2, scope3, scope4));
		when(applicationService.getApplicationById("1")).thenReturn(app1);
		when(applicationService.getApplicationById("2")).thenReturn(app2);

		mockMvc.perform(get("/portal/clients/create")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("generatedSecret"))
				.andExpect(model().attributeExists("grantTypes"))
				.andExpect(model().attribute("scopesByApplication", aMapWithSize(2)))
				.andExpect(model().attribute("scopesByApplication", hasEntry(equalTo("App One"), hasItems(scope1, scope2))))
				.andExpect(model().attribute("scopesByApplication", hasEntry(equalTo("App Two"), hasItems(scope3, scope4))))
				.andExpect(view().name("portal/create-client"));
	}

	@Test
	void testCreateClientPage_authenticated_noApplications() throws Exception {
		when(scopeService.getAllScopes()).thenReturn(List.of());

		mockMvc.perform(get("/portal/clients/create")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("generatedSecret"))
				.andExpect(model().attributeExists("grantTypes"))
				.andExpect(model().attribute("scopesByApplication", aMapWithSize(0)))
				.andExpect(view().name("portal/create-client"));
	}

	@Test
	void testCreateClientPage_unauthenticated() throws Exception {
		Scope scope = new Scope("1", "urn:cb:scope:test", "1");

		Application app = new Application("1", "App One", "A test application", "owner1");

		when(scopeService.getAllScopes()).thenReturn(List.of(scope));
		when(applicationService.getApplicationById("1")).thenReturn(app);

		mockMvc.perform(get("/portal/clients/create"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testCreateClientPage_insufficientRole() throws Exception {
		Scope scope = new Scope("1", "urn:cb:scope:test", "1");

		Application app = new Application("1", "App One", "A test application", "owner1");

		when(scopeService.getAllScopes()).thenReturn(List.of(scope));
		when(applicationService.getApplicationById("1")).thenReturn(app);

		mockMvc.perform(get("/portal/clients/create")
						.with(user("testuser").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void testEditApplicationPage_authenticated() throws Exception {
		Application app = new Application("1", "App One", "A test application", "owner1");
		Scope scope = new Scope("1", "urn:cb:scope:test", "1");

		when(applicationService.getApplicationById("1")).thenReturn(app);
		when(scopeService.getScopesByApplicationId("1")).thenReturn(List.of(scope));

		mockMvc.perform(get("/portal/applications/1")
						.with(user("testuser").roles("DEVELOPER")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("app", app))
				.andExpect(model().attribute("scopes", List.of(scope)))
				.andExpect(view().name("portal/edit-application"));
	}

	@Test
	void testEditApplicationPage_unauthenticated() throws Exception {
		mockMvc.perform(get("/portal/applications/1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testEditApplicationPage_insufficientRole() throws Exception {
		mockMvc.perform(get("/portal/applications/1")
						.with(user("testuser").roles("USER")))
				.andExpect(status().isForbidden());
	}
}