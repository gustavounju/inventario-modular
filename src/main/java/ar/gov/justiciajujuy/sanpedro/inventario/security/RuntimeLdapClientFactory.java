package ar.gov.justiciajujuy.sanpedro.inventario.security;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapRuntimeConfig;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RuntimeLdapClientFactory {

	public LdapOperations createOperations(LdapRuntimeConfig config) {
		return new LdapTemplate(createContextSource(config));
	}

	public LdapContextSource createContextSource(LdapRuntimeConfig config) {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl(config.url());
		contextSource.setBase(config.baseDn());
		if (StringUtils.hasText(config.readOnlyUserDn())) {
			contextSource.setUserDn(config.readOnlyUserDn());
			contextSource.setPassword(config.readOnlyPassword());
		}
		contextSource.afterPropertiesSet();
		return contextSource;
	}
}
