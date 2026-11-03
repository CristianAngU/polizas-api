package com.cristianangulo.polizas.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearRiesgoRequest(
        @NotBlank(message = "La dirección del inmueble es obligatoria") String direccionInmueble,
        @NotBlank(message = "El arrendatario es obligatorio") String arrendatario) { }
