package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookLlamadaDto {
    private String caller;
    private String callerName;
    private String callee;
    private String event;
    private String status;

    public String getCaller() {
        return caller;
    }
    public void setCaller(String caller) {
        this.caller = caller;
    }
    public String getCallerName() {
        return callerName;
    }
    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }
    public String getCallee() {
        return callee;
    }
    public void setCallee(String callee) {
        this.callee = callee;
    }
    public String getEvent() {
        return event;
    }
    public void setEvent(String event) {
        this.event = event;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
