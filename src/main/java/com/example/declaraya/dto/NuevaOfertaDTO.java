package com.example.declaraya.dto;

import java.math.BigDecimal;

public class NuevaOfertaDTO {

    private String mensaje;

    private BigDecimal valorPropuesto;

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public BigDecimal getValorPropuesto() {
        return valorPropuesto;
    }

    public void setValorPropuesto(BigDecimal valorPropuesto) {
        this.valorPropuesto = valorPropuesto;
    }
}
