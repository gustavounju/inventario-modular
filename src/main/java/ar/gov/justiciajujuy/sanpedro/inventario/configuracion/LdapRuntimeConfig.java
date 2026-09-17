package ar.gov.justiciajujuy.sanpedro.inventario.configuracion;

public record LdapRuntimeConfig(
		boolean enabled,
		String url,
		String domain,
		String baseDn,
		String displayNameAttribute,
		String fueroAttribute,
		String readOnlyUserDn,
		String readOnlyPassword,
		String userSearchBase,
		String userSearchFilter,
		int userSearchLimit,
		boolean persisted) {
}
