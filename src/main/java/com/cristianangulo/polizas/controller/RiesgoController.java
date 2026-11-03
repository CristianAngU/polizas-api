package com.cristianangulo.polizas.controller;

import com.cristianangulo.polizas.dto.RiesgoResponse;
import com.cristianangulo.polizas.service.PolizaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/riesgos")
public class RiesgoController {

    private final PolizaService service;

    public RiesgoController(PolizaService service) {
        this.service = service;
    }

    @PostMapping("/{id}/cancelar")
    public RiesgoResponse cancelar(@PathVariable Long id) {
        return RiesgoResponse.from(service.cancelarRiesgo(id));
    }
}
