package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.HashMap;
import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapRuntimeConfig;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RuntimeLdapClientFactory {

	private volatile LdapRuntimeConfig cachedConfig;
	private volatile LdapOperations cachedOperations;

	public LdapOperations createOperations(LdapRuntimeConfig config) {
		if (config == null) {
			return null;
		}
		LdapOperations existingOps = this.cachedOperations;
		if (existingOps != null && config.equals(this.cachedConfig)) {
			return existingOps;
		}
		synchronized (this) {
			if (this.cachedOperations != null && config.equals(this.cachedConfig)) {
				return this.cachedOperations;
			}
			LdapOperations created = new LdapTemplate(createContextSource(config));
			this.cachedConfig = config;
			this.cachedOperations = created;
			return created;
		}
	}

	public LdapContextSource createContextSource(LdapRuntimeConfig config) {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl(config.url());
		contextSource.setBase(config.baseDn());
		if (StringUtils.hasText(config.readOnlyUserDn())) {
			contextSource.setUserDn(config.readOnlyUserDn());
			contextSource.setPassword(config.readOnlyPassword());
		}
		Map<String, Object> baseEnv = new HashMap<>();
		baseEnv.put("com.sun.jndi.ldap.connect.timeout", "1200");
		baseEnv.put("com.sun.jndi.ldap.read.timeout", "2500");
		contextSource.setBaseEnvironmentProperties(baseEnv);
		contextSource.afterPropertiesSet();
		return contextSource;
	}
}
