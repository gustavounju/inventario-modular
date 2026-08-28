package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.Collection;

import ar.gov.justiciajujuy.sanpedro.inventario.config.ActiveDirectoryProperties;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ActiveDirectoryUserDetailsContextMapper implements UserDetailsContextMapper {

	private final ActiveDirectoryProperties properties;

	public ActiveDirectoryUserDetailsContextMapper(ActiveDirectoryProperties properties) {
		this.properties = properties;
	}

	@Override
	public UserDetails mapUserFromContext(
			DirContextOperations ctx,
			String username,
			Collection<? extends GrantedAuthority> authorities) {
		String displayName = firstText(ctx, properties.getDisplayNameAttribute(), username);
		String fuero = firstText(ctx, properties.getFueroAttribute(), "Sin fuero informado");
		return new ActiveDirectoryUserDetails(username, "", authorities, displayName, fuero);
	}

	@Override
	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		throw new UnsupportedOperationException("Inventario Modular solo lee usuarios desde Active Directory.");
	}

	private String firstText(DirContextOperations ctx, String attributeName, String fallback) {
		if (!StringUtils.hasText(attributeName)) {
			return fallback;
		}
		String value = ctx.getStringAttribute(attributeName);
		return StringUtils.hasText(value) ? value : fallback;
	}
}
