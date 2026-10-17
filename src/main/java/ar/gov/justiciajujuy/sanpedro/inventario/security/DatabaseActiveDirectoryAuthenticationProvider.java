package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapConfigurationService;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapRuntimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(10)
public class DatabaseActiveDirectoryAuthenticationProvider implements AuthenticationProvider {

	private final LdapConfigurationService ldapConfigurationService;
	private final ActiveDirectoryUserDetailsContextMapper userDetailsContextMapper;
	private final CredencialLocalRepository credencialLocalRepository;

	public DatabaseActiveDirectoryAuthenticationProvider(
			LdapConfigurationService ldapConfigurationService,
			ActiveDirectoryUserDetailsContextMapper userDetailsContextMapper,
			@Autowired(required = false) CredencialLocalRepository credencialLocalRepository) {
		this.ldapConfigurationService = ldapConfigurationService;
		this.userDetailsContextMapper = userDetailsContextMapper;
		this.credencialLocalRepository = credencialLocalRepository;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = String.valueOf(authentication.getPrincipal());
		// Si el usuario existe como LOCAL con credencial propia en MySQL, saltar AD directamente
		if (credencialLocalRepository != null && StringUtils.hasText(username)
				&& credencialLocalRepository.existsByUsuarioUsernameIgnoreCase(username)) {
			throw new BadCredentialsException("Usuario local con credenciales propias en base de datos.");
		}

		LdapRuntimeConfig config = ldapConfigurationService.current();
		if (!config.enabled()) {
			throw new BadCredentialsException("Active Directory no esta configurado.");
		}
		if (!StringUtils.hasText(config.url()) || !StringUtils.hasText(config.domain())
				|| !StringUtils.hasText(config.baseDn())) {
			throw new BadCredentialsException("Active Directory esta incompleto.");
		}
		// Se construye por intento para tomar la configuracion guardada en MySQL sin reiniciar.
		ActiveDirectoryLdapAuthenticationProvider provider =
				new ActiveDirectoryLdapAuthenticationProvider(config.domain(), config.url(), config.baseDn());
		provider.setConvertSubErrorCodesToExceptions(true);
		provider.setUserDetailsContextMapper(userDetailsContextMapper);
		provider.setContextEnvironmentProperties(Map.of(
				"com.sun.jndi.ldap.connect.timeout", "3000",
				"com.sun.jndi.ldap.read.timeout", "4000"));
		try {
			return provider.authenticate(authentication);
		} catch (org.springframework.security.authentication.AuthenticationServiceException e) {
			throw new BadCredentialsException("LDAP service down, falling back", e);
		} catch (AuthenticationException e) {
			throw e;
		} catch (Exception e) {
			throw new BadCredentialsException("No se pudo conectar al servidor LDAP: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}

