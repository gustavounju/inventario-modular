package ar.gov.justiciajujuy.sanpedro.inventario.config;

import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetailsContextMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ObjectProvider<AuthenticationProvider> authenticationProviders) throws Exception {
		authenticationProviders.orderedStream().forEach(http::authenticationProvider);

		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/api/v1/sistema/estado", "/css/**").permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.defaultSuccessUrl("/admin", true)
				.permitAll()
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
	@ConditionalOnProperty(name = "inventario.ldap.enabled", havingValue = "true")
	AuthenticationProvider activeDirectoryAuthenticationProvider(
			ActiveDirectoryProperties properties,
			ActiveDirectoryUserDetailsContextMapper userDetailsContextMapper) {
		ActiveDirectoryLdapAuthenticationProvider provider =
				new ActiveDirectoryLdapAuthenticationProvider(
						properties.getDomain(),
						properties.getUrl(),
						properties.getBaseDn());
		provider.setConvertSubErrorCodesToExceptions(true);
		provider.setUserDetailsContextMapper(userDetailsContextMapper);
		return provider;
	}
}
