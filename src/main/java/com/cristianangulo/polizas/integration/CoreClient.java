package com.cristianangulo.polizas.integration;

/**
 * Puerto de salida hacia el servicio agnóstico de edición del CORE (capa media WebLogic).
 * Permite cambiar la implementación (mock, REST real, cola) sin tocar la lógica de negocio.
 */
public interface CoreClient {
    void notificarEvento(String evento, Long polizaId);
}
