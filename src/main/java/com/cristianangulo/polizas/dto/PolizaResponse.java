package com.cristianangulo.polizas.dto;

import com.cristianangulo.polizas.domain.Poliza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolizaResponse(Long id, String numero, String tipo, String estado, String tomador,
                             String beneficiario, LocalDate fechaInicio, LocalDate fechaFin,
                             Integer mesesVigencia, BigDecimal canonMensual, BigDecimal prima) {

    public static PolizaResponse from(Poliza p) {
        return new PolizaResponse(p.getId(), p.getNumero(), p.getTipo().name(), p.getEstado().name(),
                p.getTomador(), p.getBeneficiario(), p.getFechaInicio(), p.getFechaFin(),
                p.getMesesVigencia(), p.getCanonMensual(), p.getPrima());
    }
}
