package ar.gov.justiciajujuy.sanpedro.inventario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ucm")
public class TelefoniaProperties {

	private boolean enabled = true;
	private String url = "https://10.15.0.2:8089/api";
	private String user = "cdrapi";
	private String password = "";
	private String interno = "1005";
	private boolean sslStrict = false;
	private String webhookIps = "10.15.0.2,127.0.0.1,0:0:0:0:0:0:0:1";

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

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getInterno() {
		return interno;
	}

	public void setInterno(String interno) {
		this.interno = interno;
	}

	public boolean isSslStrict() {
		return sslStrict;
	}

	public void setSslStrict(boolean sslStrict) {
		this.sslStrict = sslStrict;
	}

	public String getWebhookIps() {
		return webhookIps;
	}

	public void setWebhookIps(String webhookIps) {
		this.webhookIps = webhookIps;
	}
}
