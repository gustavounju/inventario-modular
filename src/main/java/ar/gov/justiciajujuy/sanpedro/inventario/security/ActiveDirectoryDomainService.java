package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import ar.gov.justiciajujuy.sanpedro.inventario.config.ActiveDirectoryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActiveDirectoryDomainService {

	private final ActiveDirectoryProperties properties;
	private final LdapOperations ldapOperations;

	@Autowired
	public ActiveDirectoryDomainService(
			ActiveDirectoryProperties properties,
			ObjectProvider<LdapOperations> ldapOperations) {
		this(properties, ldapOperations.getIfAvailable());
	}

	ActiveDirectoryDomainService(
			ActiveDirectoryProperties properties,
			LdapOperations ldapOperations) {
		this.properties = properties;
		this.ldapOperations = ldapOperations;
	}

	public DominioUsuarios listarUsuarios() {
		if (!properties.isEnabled()) {
			return DominioUsuarios.noDisponible("LDAP esta desactivado en este entorno.");
		}

		if (ldapOperations == null) {
			return DominioUsuarios.noDisponible("No hay cliente LDAP de lectura configurado.");
		}

		try {
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setCountLimit(Math.max(1, properties.getUserSearchLimit()));
			controls.setReturningAttributes(new String[] {
					"sAMAccountName",
					"userPrincipalName",
					properties.getDisplayNameAttribute(),
					properties.getFueroAttribute()
			});

			List<UsuarioDominio> usuarios = ldapOperations.search(
					properties.getUserSearchBase(),
					properties.getUserSearchFilter(),
					controls,
					(AttributesMapper<UsuarioDominio>) this::toUsuarioDominio);
			return DominioUsuarios.disponible(usuarios);
		} catch (RuntimeException exception) {
			return DominioUsuarios.noDisponible("No se pudo consultar Active Directory.");
		}
	}

	private UsuarioDominio toUsuarioDominio(Attributes attributes) throws NamingException {
		String username = firstText(attributes, "sAMAccountName", "");
		if (!StringUtils.hasText(username)) {
			username = firstText(attributes, "userPrincipalName", "");
		}
		String nombreVisible = firstText(attributes, properties.getDisplayNameAttribute(), username);
		String fuero = firstText(attributes, properties.getFueroAttribute(), "Sin fuero informado");
		return new UsuarioDominio(username, nombreVisible, fuero);
	}

	private String firstText(Attributes attributes, String attributeName, String fallback) throws NamingException {
		if (!StringUtils.hasText(attributeName)) {
			return fallback;
		}
		Attribute attribute = attributes.get(attributeName);
		if (attribute == null || attribute.get() == null) {
			return fallback;
		}
		String value = String.valueOf(attribute.get()).trim();
		return StringUtils.hasText(value) ? value : fallback;
	}

	public record DominioUsuarios(boolean disponible, String mensaje, List<UsuarioDominio> usuarios) {

		static DominioUsuarios disponible(List<UsuarioDominio> usuarios) {
			return new DominioUsuarios(true, "Active Directory disponible.", usuarios);
		}

		static DominioUsuarios noDisponible(String mensaje) {
			return new DominioUsuarios(false, mensaje, List.of());
		}
	}

	public record UsuarioDominio(String username, String nombreVisible, String fuero) {
	}
}
