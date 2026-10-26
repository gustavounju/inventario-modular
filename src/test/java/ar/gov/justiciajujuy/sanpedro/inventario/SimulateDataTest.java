package ar.gov.justiciajujuy.sanpedro.inventario;

import ar.gov.justiciajujuy.sanpedro.inventario.componentes.TipoComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.EstadoStockComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Script utilitario manual para insertar datos simulados en base local MySQL")
@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    // El perfil casa ejecuta SecurityStartupValidator: requiere clave de recordarme (solo de prueba).
    "inventario.security.remember-me-key=clave-recordarme-solo-para-tests-0123456789abcdef"
})
@ActiveProfiles("casa")
class SimulateDataTest {

    @Autowired
    private StockService stockService;

    @Test
    void insertSimulatedData() {
        stockService.crear(new StockService.GuardarStockComponenteCommand(
            TipoComponente.RAM,
            EstadoStockComponente.DISPONIBLE,
            "Memoria RAM DDR4 8GB",
            "Corsair",
            "Vengeance",
            "SN-RAM-LIBRE-01",
            "8GB",
            null, null, null, "admin", null, null,
            true
        ));

        stockService.crear(new StockService.GuardarStockComponenteCommand(
            TipoComponente.DISCO,
            EstadoStockComponente.DISPONIBLE,
            "Disco SSD 480GB",
            "Kingston",
            "A400",
            "SN-SSD-LIBRE-01",
            "480GB",
            null, null, null, "admin", null, null,
            true
        ));

        stockService.crear(new StockService.GuardarStockComponenteCommand(
            TipoComponente.TECLADO,
            EstadoStockComponente.DISPONIBLE,
            "Teclado USB",
            "Logitech",
            "K120",
            "SN-KBD-LIBRE-01",
            "Latinoamericano",
            null, null, null, "admin", null, null,
            true
        ));

        System.out.println("Componentes de stock insertados correctamente.");
    }
}
