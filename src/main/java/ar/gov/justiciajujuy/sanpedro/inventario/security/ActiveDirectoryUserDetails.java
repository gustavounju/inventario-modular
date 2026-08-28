package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class ActiveDirectoryUserDetails extends User {

	private final String displayName;
	private final String fuero;

	public ActiveDirectoryUserDetails(
			String username,
			String password,
			Collection<? extends GrantedAuthority> authorities,
			String displayName,
			String fuero) {
		super(username, password, authorities);
		this.displayName = displayName;
		this.fuero = fuero;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFuero() {
		return fuero;
	}
}
