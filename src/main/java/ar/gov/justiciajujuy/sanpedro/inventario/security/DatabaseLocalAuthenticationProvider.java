package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.List;
import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapConfigurationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@ConditionalOnProperty(name = "inventario.local-db-auth.enabled", havingValue = "true")
public class DatabaseLocalAuthenticationProvider implements AuthenticationProvider {

	private final CredencialLocalRepository credencialLocalRepository;
	private final PasswordEncoder passwordEncoder;
	private final LdapConfigurationService ldapConfigurationService;

	public DatabaseLocalAuthenticationProvider(
			CredencialLocalRepository credencialLocalRepository,
			PasswordEncoder passwordEncoder,
			LdapConfigurationService ldapConfigurationService) {
		this.credencialLocalRepository = credencialLocalRepository;
		this.passwordEncoder = passwordEncoder;
		this.ldapConfigurationService = ldapConfigurationService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = String.valueOf(authentication.getPrincipal());
		String password = String.valueOf(authentication.getCredentials());
		if ("admin.local".equalsIgnoreCase(username.trim()) && ldapConfigurationService.persistentAdEnabled()) {
			// admin.local es bootstrap: sirve para configurar AD, no para convivir con AD ya persistido.
			throw new BadCredentialsException("admin.local queda deshabilitado cuando Active Directory esta configurado.");
		}
		CredencialLocal credencial = credencialLocalRepository.findActivaByUsername(username)
				.orElseThrow(() -> new BadCredentialsException("Credenciales invalidas."));

		if (!passwordEncoder.matches(password, credencial.getPasswordHash())) {
			throw new BadCredentialsException("Credenciales invalidas.");
		}

		UsuarioSistema usuario = credencial.getUsuario();
		ActiveDirectoryUserDetails principal = new ActiveDirectoryUserDetails(
				usuario.getUsername(),
				credencial.getPasswordHash(),
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				usuario.getNombreVisible(),
				usuario.getFuero(),
				Map.of(
						"modo", List.of("LOCAL_DB"),
						"origen", List.of("Usuario local con credencial propia")));

		return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
