package com.cristianangulo.polizas.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(int status, String error, String mensaje, String path, OffsetDateTime timestamp) { }
