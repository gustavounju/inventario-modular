package ar.gov.justiciajujuy.sanpedro.inventario.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ar.gov.justiciajujuy.sanpedro.inventario.security.SoloMovilAccessInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final SoloMovilAccessInterceptor soloMovilAccessInterceptor;

	public WebMvcConfig(SoloMovilAccessInterceptor soloMovilAccessInterceptor) {
		this.soloMovilAccessInterceptor = soloMovilAccessInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(soloMovilAccessInterceptor)
				.addPathPatterns("/admin", "/admin/**")
				.excludePathPatterns("/admin/tareas/visor", "/admin/tareas/visor/**");
	}
}
