package com.connellboyce.authhub.config;

import com.connellboyce.authhub.filter.AuthorizationRequestFilter;
import com.connellboyce.authhub.grant.TokenExchangeAuthenticationProvider;
import com.connellboyce.authhub.model.Actor;
import com.connellboyce.authhub.model.ActorType;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import com.connellboyce.authhub.repository.RegisteredClientRepositoryImpl;
import com.connellboyce.authhub.repository.UserRepository;
import com.connellboyce.authhub.service.UserDetailsServiceImpl;
import com.connellboyce.authhub.service.UserService;
import com.connellboyce.authhub.util.CBRole;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2TokenExchangeAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.connellboyce.authhub.util.RsaUtils.generateRsaKey;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class WebSecurityConfig {
	private static final String ISSUER = "https://auth.connellboyce.com";
	private static final String CLIENTS_PATH = "/api/v1/clients/**";
	private static final String APPLICATIONS_PATH = "/api/v1/applications/**";
	private static final String SCOPES_PATH = "/api/v1/scopes/**";
	private static final String CLIENTS_READ = "SCOPE_urn:cb:scope:authhub:clients#read";
	private static final String CLIENTS_WRITE = "SCOPE_urn:cb:scope:authhub:clients#write";
	private static final String APPLICATIONS_READ = "SCOPE_urn:cb:scope:authhub:applications#read";
	private static final String APPLICATIONS_WRITE = "SCOPE_urn:cb:scope:authhub:applications#write";
	private static final String SCOPES_READ = "SCOPE_urn:cb:scope:authhub:scopes#read";

	@Value("#{'${spring.security.login.entry-point.preserved-params}'.split(',')}")
	Set<String> preservedParams;

	private static String clientRegistrationEndpoint() {
		return AuthorizationServerContextHolder.getContext().getIssuer() + "/api/v1/clients";
	}

	private static AuthorizationManager<RequestAuthorizationContext> developerWithScope(String scopeAuthority) {
		return AuthorizationManagers.allOf(
				AuthorityAuthorizationManager.hasRole(CBRole.ROLE_DEVELOPER.withoutPrefix()),
				AuthorityAuthorizationManager.hasAuthority(scopeAuthority)
		);
	}

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
	                                                           Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper,
	                                                           OAuth2AuthorizationService authorizationService,
	                                                           OAuth2TokenGenerator<?> tokenGenerator,
	                                                           JwtDecoder jwtDecoder,
	                                                           RegisteredClientRepository registeredClientRepository)
			throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
				.authorizationServerMetadataEndpoint(metadata -> metadata
						.authorizationServerMetadataCustomizer(builder -> builder.clientRegistrationEndpoint(clientRegistrationEndpoint()))
				)
				.oidc((oidc) -> oidc
						.providerConfigurationEndpoint(providerConfiguration -> providerConfiguration
								.providerConfigurationCustomizer(builder -> builder.clientRegistrationEndpoint(clientRegistrationEndpoint()))
						)
						.userInfoEndpoint((userInfo) -> userInfo
								.userInfoMapper(oidcUserInfoMapper)
						)
				)
				.tokenEndpoint(tokenEndpoint -> tokenEndpoint
						.accessTokenRequestConverters(converters ->
								converters.add(new OAuth2TokenExchangeAuthenticationConverter())
						)
						.authenticationProviders(providers -> {
									providers.removeIf(OAuth2TokenExchangeAuthenticationProvider.class::isInstance);
									providers.add(0, new TokenExchangeAuthenticationProvider(jwtDecoder, authorizationService, tokenGenerator, registeredClientRepository));
								}
						)
				);
		http
				.exceptionHandling((exceptions) -> exceptions
						.defaultAuthenticationEntryPointFor(
								new LoginUrlAuthenticationEntryPoint("/login"),
								new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
						)
				)
				.oauth2ResourceServer((resourceServer) -> resourceServer
						.jwt(withDefaults()));

		http.addFilterBefore(new AuthorizationRequestFilter(preservedParams), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain managementApiSecurityFilterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
		http
				.securityMatcher(CLIENTS_PATH, APPLICATIONS_PATH, SCOPES_PATH)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, CLIENTS_PATH).access(developerWithScope(CLIENTS_READ))
						.requestMatchers(HttpMethod.POST, CLIENTS_PATH).access(developerWithScope(CLIENTS_WRITE))
						.requestMatchers(HttpMethod.PUT, CLIENTS_PATH).access(developerWithScope(CLIENTS_WRITE))
						.requestMatchers(HttpMethod.DELETE, CLIENTS_PATH).access(developerWithScope(CLIENTS_WRITE))
						.requestMatchers(HttpMethod.GET, APPLICATIONS_PATH).access(developerWithScope(APPLICATIONS_READ))
						.requestMatchers(HttpMethod.POST, APPLICATIONS_PATH).access(developerWithScope(APPLICATIONS_WRITE))
						.requestMatchers(HttpMethod.PUT, APPLICATIONS_PATH).access(developerWithScope(APPLICATIONS_WRITE))
						.requestMatchers(HttpMethod.DELETE, APPLICATIONS_PATH).access(developerWithScope(APPLICATIONS_WRITE))
						.requestMatchers(HttpMethod.GET, SCOPES_PATH).access(developerWithScope(SCOPES_READ))
						.anyRequest().denyAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(AbstractHttpConfigurer::disable)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt
								.jwtAuthenticationConverter(new ManagementJwtAuthenticationConverter(userRepository))));
		return http.build();
	}

	@Bean
	@Order(3)
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/api/v1/**")
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/v1/user").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/user"));
		return http.build();
	}

	@Bean
	@Order(4)
	SecurityFilterChain uiSecurityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
			throws Exception {
		http
				.securityMatcher("/portal/**", "/portal", "/login", "/register")
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/login", "/register").permitAll()
						.requestMatchers("/portal/**", "/portal").hasRole("DEVELOPER"))
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
				.userDetailsService(userDetailsService)
				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.permitAll()
				);
		return http.build();
	}

	@Bean
	@Order(5)
	SecurityFilterChain publicResourcesSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/error",
								"/actuator/**",
								"/.well-known/**",
								"/webjars/**",
								"/images/**",
								"/css/**",
								"/assets/**",
								"/favicon.ico"
						).permitAll()
						.anyRequest().denyAll());
		return http.build();
	}

	@Bean
	public RegisteredClientRepository registeredClientRepository(MongoRegisteredClientRepository repository) {
		return new RegisteredClientRepositoryImpl(repository);
	}

	@Bean
	public UserDetailsService userDetailsService(UserDetailsServiceImpl userDetailsService) {
		return userDetailsService;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		KeyPair keyPair = generateRsaKey();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		RSAKey rsaKey = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return new ImmutableJWKSet<>(jwkSet);
	}

	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource, AuthorizationServerSettings authorizationServerSettings) {
		NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
		jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(authorizationServerSettings.getIssuer()));
		return jwtDecoder;
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().issuer(ISSUER).build();
	}

	@Bean
	public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new HiddenHttpMethodFilter();
	}

	@Bean
	public OAuth2AuthorizationService authorizationService() {
		return new InMemoryOAuth2AuthorizationService();
	}

	@Bean
	public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource, OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer) {
		JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
		JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
		jwtGenerator.setJwtCustomizer(tokenCustomizer);

		OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
		OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

		return new DelegatingOAuth2TokenGenerator(jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
	}
}
