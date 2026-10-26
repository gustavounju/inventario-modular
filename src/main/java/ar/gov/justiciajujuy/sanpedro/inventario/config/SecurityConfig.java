package ar.gov.justiciajujuy.sanpedro.inventario.config;

import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.LanOnlyAccessFilter;
import ar.gov.justiciajujuy.sanpedro.inventario.security.LoginAttemptService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.LoginRateLimitFilter;
import ar.gov.justiciajujuy.sanpedro.inventario.security.SetupFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import ar.gov.justiciajujuy.sanpedro.inventario.security.TokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

	@Value("${inventario.security.report-token:}")
	private String reportToken;

	// Sin valor por defecto: SecurityStartupValidator aborta el arranque si falta o es corta.
	@Value("${inventario.security.remember-me-key:}")
	private String rememberMeKey;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			NetworkAccessProperties networkAccessProperties,
			ObjectProvider<AuthenticationProvider> authenticationProviders,
			LoginAttemptService loginAttemptService,
			AuthorizationService authorizationService,
			SetupFilter setupFilter) throws Exception {
		authenticationProviders.orderedStream().forEach(http::authenticationProvider);

		http
			.addFilterBefore(setupFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new LanOnlyAccessFilter(networkAccessProperties), UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new TokenAuthenticationFilter(reportToken), UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new LoginRateLimitFilter(loginAttemptService), UsernamePasswordAuthenticationFilter.class)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/api/v1/**", "/submit_inventory", "/api/telefonia/webhook/llamada")
			)
			.headers(headers -> headers
				.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
				.contentTypeOptions(contentType -> {})
				.frameOptions(frame -> frame.sameOrigin())
			)
			.exceptionHandling(exceptions -> exceptions
				.defaultAuthenticationEntryPointFor(
					new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
					request -> request.getRequestURI().startsWith("/api/") || "/submit_inventory".equals(request.getRequestURI())
				)
				.defaultAuthenticationEntryPointFor(
					new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/movil/login"),
					request -> request.getRequestURI().contains("/movil")
				)
				.defaultAuthenticationEntryPointFor(
					new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login"),
					org.springframework.security.web.util.matcher.AnyRequestMatcher.INSTANCE
				)
			)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/", "/login", "/setup", "/movil/login", "/logout",
					"/css/**", "/js/**", "/images/**", "/scripts/**", "/webjars/**", "/favicon.ico", "/app/**",
					"/manifest.json", "/icons/**"
				).permitAll()
				// El visor de tareas es de solo lectura y publico a proposito para monitores/pantallas en guardia o taller sin requerir inicio de sesion.
				.requestMatchers(HttpMethod.GET, "/admin/tareas/visor").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/movil/tecnicos-conectados").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/telefonia/webhook/llamada").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/telefonia/stream", "/api/telefonia/historial-hoy", "/api/telefonia/directorio").permitAll()
				.requestMatchers("/submit_inventory").authenticated()
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/login")
				.successHandler((request, response, authentication) -> {
					// Destinos internos cerrados: el parametro del formulario nunca se usa como URL arbitraria.
					loginAttemptService.loginSucceeded(request.getParameter("username"), request.getRemoteAddr());
					boolean esSoloMovil = false;
					if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
						esSoloMovil = authorizationService.esSoloMovil(userDetails);
					}
					String destino = ("movil".equals(request.getParameter("destino")) || esSoloMovil) ? "/movil/tareas" : "/admin";
					new org.springframework.security.web.savedrequest.HttpSessionRequestCache().removeRequest(request, response);
					response.sendRedirect(request.getContextPath() + destino);
				})
				.failureHandler((request, response, exception) -> {
					loginAttemptService.loginFailed(request.getParameter("username"), request.getRemoteAddr());
					LOGGER.warn("Login fallido para usuario {} desde {}: {}",
							request.getParameter("username"),
							request.getRemoteAddr(),
							exception.getMessage());
					response.sendRedirect(request.getContextPath()
							+ ("movil".equals(request.getParameter("destino")) ? "/movil/login?error" : "/login?error"));
				})
				.permitAll()
			)
			.rememberMe(remember -> remember
				// Si falta la clave, Spring genera una aleatoria por arranque (nunca fija) y el validador aborta fuera de tests.
				.key(StringUtils.hasText(rememberMeKey) ? rememberMeKey : null)
				.alwaysRemember(true)
				.tokenValiditySeconds(86400 * 30) // 30 dias de persistencia
			)
			.logout(logout -> logout
				.logoutSuccessUrl("/")
				.permitAll()
			);

		return http.build();
	}

	@Bean
	@ConditionalOnProperty(name = "inventario.ldap.enabled", havingValue = "true")
	LdapOperations activeDirectoryReadOnlyLdapOperations(ActiveDirectoryProperties properties) {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl(properties.getUrl());
		contextSource.setBase(properties.getBaseDn());
		if (StringUtils.hasText(properties.getReadOnlyUserDn())) {
			contextSource.setUserDn(properties.getReadOnlyUserDn());
			contextSource.setPassword(properties.getReadOnlyPassword());
		}
		contextSource.afterPropertiesSet();
		return new LdapTemplate(contextSource);
	}

	@Bean
	public org.springframework.security.core.userdetails.UserDetailsService userDetailsService(
			ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioSistemaRepository usuarioRepository) {
		return username -> {
			ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioSistema usuario = usuarioRepository.findByUsernameIgnoreCase(username)
					.orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Usuario no encontrado: " + username));
			if (!usuario.isActivo()) {
				throw new org.springframework.security.authentication.DisabledException("El usuario '" + username + "' se encuentra inactivo.");
			}

			java.util.List<String> roleCodes = usuarioRepository.findRoleCodesByUsername(usuario.getUsername());
			java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
			authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
			for (String code : roleCodes) {
				if (StringUtils.hasText(code)) {
					String rolNormalizado = code.trim().toUpperCase();
					authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rolNormalizado));
					authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(rolNormalizado));
				}
			}

			return new ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails(
					usuario.getUsername(),
					"{noop}", 
					authorities,
					usuario.getNombreVisible(),
					usuario.getFuero(),
					java.util.Map.of("modo", java.util.List.of("PERSISTENT_COOKIE"))
			);
		};
	}

}
