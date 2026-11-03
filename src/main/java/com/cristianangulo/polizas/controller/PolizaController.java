package com.cristianangulo.polizas.controller;

import com.cristianangulo.polizas.domain.EstadoPoliza;
import com.cristianangulo.polizas.domain.TipoPoliza;
import com.cristianangulo.polizas.dto.CrearRiesgoRequest;
import com.cristianangulo.polizas.dto.PolizaResponse;
import com.cristianangulo.polizas.dto.RiesgoResponse;
import com.cristianangulo.polizas.service.PolizaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService service;

    public PolizaController(PolizaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                       @RequestParam(required = false) EstadoPoliza estado) {
        return service.listar(tipo, estado).stream().map(PolizaResponse::from).toList();
    }

    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> riesgos(@PathVariable Long id) {
        return service.listarRiesgos(id).stream().map(RiesgoResponse::from).toList();
    }

    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable Long id) {
        return PolizaResponse.from(service.renovar(id));
    }

    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return PolizaResponse.from(service.cancelar(id));
    }

    @PostMapping("/{id}/riesgos")
    @ResponseStatus(HttpStatus.CREATED)
    public RiesgoResponse agregarRiesgo(@PathVariable Long id, @Valid @RequestBody CrearRiesgoRequest request) {
        return RiesgoResponse.from(service.agregarRiesgo(id, request));
    }
}
