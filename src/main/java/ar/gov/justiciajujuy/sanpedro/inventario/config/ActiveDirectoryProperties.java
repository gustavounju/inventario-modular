package ar.gov.justiciajujuy.sanpedro.inventario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventario.ldap")
public class ActiveDirectoryProperties {

	private boolean enabled;
	private String url = "ldap://SERVIDOR_AD:389";
	private String domain = "DOMINIO";
	private String baseDn = "DC=ejemplo,DC=local";
	private String displayNameAttribute = "displayName";
	private String fueroAttribute = "department";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getBaseDn() {
		return baseDn;
	}

	public void setBaseDn(String baseDn) {
		this.baseDn = baseDn;
	}

	public String getDisplayNameAttribute() {
		return displayNameAttribute;
	}

	public void setDisplayNameAttribute(String displayNameAttribute) {
		this.displayNameAttribute = displayNameAttribute;
	}

	public String getFueroAttribute() {
		return fueroAttribute;
	}

	public void setFueroAttribute(String fueroAttribute) {
		this.fueroAttribute = fueroAttribute;
	}
}
