package ar.gov.justiciajujuy.sanpedro.inventario.config;

import java.util.List;
import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Configura el login local de desarrollo cuando no hay Active Directory disponible.
 *
 * <p>La clase se activa solo si `inventario.local-auth.enabled=true`. Ademas, sus beans
 * se registran solo cuando `inventario.ldap.enabled=false`, para que el modo de casa no
 * compita con el login real del dominio en el trabajo.</p>
 */
@Configuration
@ConditionalOnProperty(name = "inventario.local-auth.enabled", havingValue = "true")
public class LocalAuthenticationConfig {

	@Bean
	@ConditionalOnProperty(name = "inventario.ldap.enabled", havingValue = "false", matchIfMissing = true)
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@ConditionalOnProperty(name = "inventario.ldap.enabled", havingValue = "false", matchIfMissing = true)
	UserDetailsService localUserDetailsService(LocalAuthenticationProperties properties, PasswordEncoder passwordEncoder) {
		/*
		 * Este usuario existe solo para estudiar y probar en casa, donde no hay acceso al
		 * dominio real. Reutilizamos ActiveDirectoryUserDetails para que la pantalla vea
		 * una identidad parecida a la que recibe cuando el login viene desde AD. La clave
		 * se codifica al arrancar para evitar ejemplos con password en texto plano dentro
		 * de Spring Security.
		 */
		String configuredUsername = properties.getUsername();
		String encodedPassword = passwordEncoder.encode(properties.getPassword());

		return username -> {
			if (StringUtils.hasText(username) && configuredUsername.equalsIgnoreCase(username.trim())) {
				/*
				 * Spring Security borra las credenciales del UserDetails autenticado.
				 * Por eso devolvemos una instancia nueva en cada busqueda y conservamos
				 * la clave codificada fuera del objeto que se entrega al framework.
				 */
				return new ActiveDirectoryUserDetails(
						properties.getUsername(),
						encodedPassword,
						List.of(
								new SimpleGrantedAuthority("ROLE_USER"),
								new SimpleGrantedAuthority("ROLE_ADMIN")),
						properties.getDisplayName(),
						properties.getFuero(),
						Map.of(
								"modo", List.of("LOCAL_SIMULADO"),
								"origen", List.of("Windows sin dominio"),
								"descripcion", List.of("Usuario local para desarrollo")));
			}
			throw new UsernameNotFoundException("Usuario local no configurado: " + username);
		};
	}
}
