package ar.gov.justiciajujuy.sanpedro.inventario.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/admin", "/api/v1/sistema/estado", "/css/**").permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(form -> form.permitAll());

		return http.build();
	}
}
