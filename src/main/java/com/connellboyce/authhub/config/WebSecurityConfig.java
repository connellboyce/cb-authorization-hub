package com.connellboyce.authhub.config;

import com.connellboyce.authhub.filter.AuthorizationRequestFilter;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import com.connellboyce.authhub.repository.RegisteredClientRepositoryImpl;
import com.connellboyce.authhub.service.UserDetailsServiceImpl;
import com.connellboyce.authhub.service.UserService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.connellboyce.authhub.util.RsaUtils.generateRsaKey;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class WebSecurityConfig {

	@Value("#{'${spring.security.login.entry-point.preserved-params}'.split(',')}")
	Set<String> preservedParams;

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http, Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper)
			throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
				.oidc((oidc) -> oidc
						.userInfoEndpoint((userInfo) -> userInfo
								.userInfoMapper(oidcUserInfoMapper)
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
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
			throws Exception {
		http
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers("/error").permitAll()
						.requestMatchers("/api/v1/user").permitAll()
						.requestMatchers("/register").permitAll()
						.requestMatchers("/login").permitAll()
						.requestMatchers("/portal/**").hasRole("DEVELOPER")
						.requestMatchers("/portal").hasRole("DEVELOPER")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
				.userDetailsService(userDetailsService)
				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.permitAll()
				);
		return http.build();
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.debug(false)
				.ignoring()
				.requestMatchers(
						"/webjars/**",
						"/images/**",
						"/css/**",
						"/assets/**",
						"/favicon.ico",
						"/.well-known/robots.txt",
						"/.well-known/humans.txt",
						"/actuator/**",
						"/api/v1/user"
				);
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
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}

	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(UserService userService) {
		return context -> {
			Authentication principal = context.getPrincipal();
			if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				Set<String> authorities = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority)
						.collect(Collectors.toSet());
				context.getClaims().claim("role", authorities);
				context.getClaims().claim("scope", context.getAuthorizedScopes());
				if (principal.getPrincipal() instanceof User user) {
					context.getClaims().subject(userService.getCBUserByUsername(principal.getName()).getId());
					context.getClaims().claim("username", user.getUsername());
					context.getClaims().claim("amr", Set.of("pwd"));
				}
				context.getClaims().claim("azp", context.getRegisteredClient().getClientId());
			}
		};
	}

	@Bean
	public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new HiddenHttpMethodFilter();
	}
}
