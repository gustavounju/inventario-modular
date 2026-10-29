package ar.gov.justiciajujuy.sanpedro.inventario;

import java.util.TimeZone;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InventarioModularApplication {

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Jujuy"));
	}

	public static void main(String[] args) {
		SpringApplication.run(InventarioModularApplication.class, args);
	}

}
