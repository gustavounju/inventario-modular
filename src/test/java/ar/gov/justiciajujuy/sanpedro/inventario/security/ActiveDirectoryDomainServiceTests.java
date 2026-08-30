package ar.gov.justiciajujuy.sanpedro.inventario.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;

import ar.gov.justiciajujuy.sanpedro.inventario.config.ActiveDirectoryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapOperations;

class ActiveDirectoryDomainServiceTests {

	@Test
	void noConsultaLdapCuandoEstaDesactivado() {
		ActiveDirectoryProperties properties = new ActiveDirectoryProperties();
		LdapOperations ldapOperations = mock(LdapOperations.class);
		ActiveDirectoryDomainService service = new ActiveDirectoryDomainService(properties, ldapOperations);

		ActiveDirectoryDomainService.DominioUsuarios resultado = service.listarUsuarios();

		assertThat(resultado.disponible()).isFalse();
		assertThat(resultado.mensaje()).isEqualTo("LDAP esta desactivado en este entorno.");
		assertThat(resultado.usuarios()).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void mapeaUsuariosDelDirectorioSinClavesNiAtributosSensibles() throws Exception {
		ActiveDirectoryProperties properties = new ActiveDirectoryProperties();
		properties.setEnabled(true);
		properties.setUserSearchBase("OU=Usuarios");
		properties.setUserSearchFilter("(objectClass=user)");
		LdapOperations ldapOperations = mock(LdapOperations.class);
		BasicAttributes attributes = new BasicAttributes();
		attributes.put(new BasicAttribute("sAMAccountName", "gmurad"));
		attributes.put(new BasicAttribute("displayName", "Gustavo Elias Murad"));
		attributes.put(new BasicAttribute("department", "Informatica"));

		when(ldapOperations.search(
				eq("OU=Usuarios"),
				eq("(objectClass=user)"),
				any(SearchControls.class),
				any(AttributesMapper.class)))
			.thenAnswer(invocation -> List.of(
					((AttributesMapper<ActiveDirectoryDomainService.UsuarioDominio>) invocation.getArgument(3))
							.mapFromAttributes(attributes)));

		ActiveDirectoryDomainService service = new ActiveDirectoryDomainService(properties, ldapOperations);

		ActiveDirectoryDomainService.DominioUsuarios resultado = service.listarUsuarios();

		assertThat(resultado.disponible()).isTrue();
		assertThat(resultado.usuarios()).containsExactly(
				new ActiveDirectoryDomainService.UsuarioDominio("gmurad", "Gustavo Elias Murad", "Informatica"));
	}
}
