package com.cristianangulo.polizas.controller;

import com.cristianangulo.polizas.dto.CoreEventoRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mock del servicio de edición del CORE: solo registra en logs el intento de envío.
 */
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    public ResponseEntity<Map<String, Object>> recibirEvento(@Valid @RequestBody CoreEventoRequest request) {
        log.info("[CORE-MOCK] Se intentó enviar al CORE el evento {} de la póliza {}", request.evento(), request.polizaId());
        return ResponseEntity.accepted().body(Map.of("recibido", true, "evento", request.evento(), "polizaId", request.polizaId()));
    }
}
