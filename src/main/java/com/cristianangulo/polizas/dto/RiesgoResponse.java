package com.cristianangulo.polizas.dto;

import com.cristianangulo.polizas.domain.Riesgo;

public record RiesgoResponse(Long id, Long polizaId, String direccionInmueble, String arrendatario, String estado) {

    public static RiesgoResponse from(Riesgo r) {
        return new RiesgoResponse(r.getId(), r.getPoliza().getId(), r.getDireccionInmueble(),
                r.getArrendatario(), r.getEstado().name());
    }
}
