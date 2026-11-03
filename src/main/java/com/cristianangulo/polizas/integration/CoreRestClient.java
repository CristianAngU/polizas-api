package com.cristianangulo.polizas.integration;

import com.cristianangulo.polizas.config.ApiKeyFilter;
import com.cristianangulo.polizas.dto.CoreEventoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptador REST hacia el CORE. Si el CORE falla, se registra el error sin bloquear
 * la operación de negocio (en producción se reintentaría o se enviaría a una cola).
 */
@Component
public class CoreRestClient implements CoreClient {

    private static final Logger log = LoggerFactory.getLogger(CoreRestClient.class);

    private final RestClient restClient;
    private final String coreUrl;
    private final String apiKey;

    public CoreRestClient(@Value("${app.core.url}") String coreUrl,
                          @Value("${app.security.api-key}") String apiKey) {
        this.restClient = RestClient.create();
        this.coreUrl = coreUrl;
        this.apiKey = apiKey;
    }

    @Override
    public void notificarEvento(String evento, Long polizaId) {
        try {
            restClient.post()
                    .uri(coreUrl)
                    .header(ApiKeyFilter.HEADER, apiKey)
                    .body(new CoreEventoRequest(evento, polizaId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("No fue posible enviar el evento {} de la póliza {} al CORE: {}", evento, polizaId, ex.getMessage());
        }
    }
}
