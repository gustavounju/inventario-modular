package ar.gov.justiciajujuy.sanpedro.inventario.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import ar.gov.justiciajujuy.sanpedro.inventario.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UserDetailsServiceTests {

	@Test
	void cargaUsuarioActivoConRolesYAutoridades() {
		UsuarioSistemaRepository repo = mock(UsuarioSistemaRepository.class);
		UsuarioSistema usuario = new UsuarioSistema("tecnico.guardia", "Tecnico de Guardia", "Taller");
		usuario.setActivo(true);

		when(repo.findByUsernameIgnoreCase("tecnico.guardia")).thenReturn(Optional.of(usuario));
		when(repo.findRoleCodesByUsername("tecnico.guardia")).thenReturn(List.of("TECNICO", "ADMINISTRADOR"));

		SecurityConfig config = new SecurityConfig();
		UserDetailsService service = config.userDetailsService(repo);

		UserDetails userDetails = service.loadUserByUsername("tecnico.guardia");

		assertThat(userDetails).isNotNull();
		assertThat(userDetails.getUsername()).isEqualTo("tecnico.guardia");
		assertThat(userDetails.isEnabled()).isTrue();

		List<String> authorities = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		assertThat(authorities).contains("ROLE_USER", "ROLE_TECNICO", "TECNICO", "ROLE_ADMINISTRADOR", "ADMINISTRADOR");
	}

	@Test
	void rechazaUsuarioInactivoConDisabledException() {
		UsuarioSistemaRepository repo = mock(UsuarioSistemaRepository.class);
		UsuarioSistema usuario = new UsuarioSistema("usuario.baja", "Usuario Dado de Baja", "Civil");
		usuario.setActivo(false);

		when(repo.findByUsernameIgnoreCase("usuario.baja")).thenReturn(Optional.of(usuario));

		SecurityConfig config = new SecurityConfig();
		UserDetailsService service = config.userDetailsService(repo);

		assertThatThrownBy(() -> service.loadUserByUsername("usuario.baja"))
				.isInstanceOf(DisabledException.class)
				.hasMessageContaining("inactivo");
	}

	@Test
	void rechazaUsuarioInexistenteConUsernameNotFoundException() {
		UsuarioSistemaRepository repo = mock(UsuarioSistemaRepository.class);
		when(repo.findByUsernameIgnoreCase("desconocido")).thenReturn(Optional.empty());

		SecurityConfig config = new SecurityConfig();
		UserDetailsService service = config.userDetailsService(repo);

		assertThatThrownBy(() -> service.loadUserByUsername("desconocido"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Usuario no encontrado");
	}
}
