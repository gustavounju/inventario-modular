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
			LdapTemplate template = new LdapTemplate(createContextSource(config));
			template.setIgnorePartialResultException(true);
			template.setIgnoreNameNotFoundException(true);
			template.setIgnoreSizeLimitExceededException(true);
			this.cachedConfig = config;
			this.cachedOperations = template;
			return template;
		}
	}

	public LdapContextSource createContextSource(LdapRuntimeConfig config) {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl(config.url());
		contextSource.setBase(config.baseDn());
		contextSource.setReferral("ignore");
		contextSource.setAnonymousReadOnly(false);
		if (StringUtils.hasText(config.readOnlyUserDn())) {
			contextSource.setUserDn(config.readOnlyUserDn());
			contextSource.setPassword(config.readOnlyPassword());
		}
		Map<String, Object> baseEnv = new HashMap<>();
		baseEnv.put("com.sun.jndi.ldap.connect.timeout", "4000");
		baseEnv.put("com.sun.jndi.ldap.read.timeout", "8000");
		contextSource.setBaseEnvironmentProperties(baseEnv);
		contextSource.afterPropertiesSet();
		return contextSource;
	}
}
