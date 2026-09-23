package ar.gov.justiciajujuy.sanpedro.inventario.security;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapConfigurationService;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapRuntimeConfig;
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

	public DatabaseActiveDirectoryAuthenticationProvider(
			LdapConfigurationService ldapConfigurationService,
			ActiveDirectoryUserDetailsContextMapper userDetailsContextMapper) {
		this.ldapConfigurationService = ldapConfigurationService;
		this.userDetailsContextMapper = userDetailsContextMapper;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
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
