package ar.gov.justiciajujuy.sanpedro.inventario.red;

public enum CategoriaIp {
    ROUTER("Router de Core / Borde"),
    SWITCH("Switch"),
    ACCESS_POINT("Access Point Wi-Fi"),
    SERVIDOR("Servidor Físico / Virtual"),
    RELOJ_BIOMETRICO("Reloj Facial / Biométrico"),
    TERMINAL("Terminal / PC de Monitoreo / Totem"),
    IMPRESORA("Impresora de Red"),
    OTRO("Otro Dispositivo");

    private final String descripcion;

    CategoriaIp(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
