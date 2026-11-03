package com.cristianangulo.polizas.controller;

import com.cristianangulo.polizas.domain.Poliza;
import com.cristianangulo.polizas.domain.Riesgo;
import com.cristianangulo.polizas.domain.TipoPoliza;
import com.cristianangulo.polizas.integration.CoreClient;
import com.cristianangulo.polizas.repository.PolizaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PolizaControllerTest {

    private static final String KEY = "x-api-key";

    @Autowired MockMvc mvc;
    @Autowired PolizaRepository repo;
    @MockBean CoreClient coreClient;

    Long individualId;
    Long colectivaId;
    Long riesgoIndividualId;

    @BeforeEach
    void datos() {
        repo.deleteAll();
        Poliza ind = new Poliza("IND-T", TipoPoliza.INDIVIDUAL, "T", "B", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
        Riesgo riesgo = new Riesgo("Dir", "A");
        ind.agregarRiesgo(riesgo);
        Poliza col = new Poliza("COL-T", TipoPoliza.COLECTIVA, "T", "B", LocalDate.of(2026, 1, 1), 12, new BigDecimal("2000000"));
        individualId = repo.save(ind).getId();
        riesgoIndividualId = riesgo.getId();
        colectivaId = repo.save(col).getId();
    }

    @Test
    void sinApiKeyRetorna401() throws Exception {
        mvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Header x-api-key inválido o ausente"))
                .andExpect(jsonPath("$.path").value("/polizas"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void polizaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/polizas/{id}/riesgos", 999).header(KEY, "123456"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("No existe la póliza con id 999"))
                .andExpect(jsonPath("$.path").value("/polizas/999/riesgos"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void cancelarRiesgoLoDejaCancelado() throws Exception {
        mvc.perform(post("/riesgos/{id}/cancelar", riesgoIndividualId).header(KEY, "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.id").value(riesgoIndividualId));
    }

    @Test
    void listarFiltraPorTipo() throws Exception {
        mvc.perform(get("/polizas").param("tipo", "COLECTIVA").header(KEY, "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("COLECTIVA"));
    }

    @Test
    void agregarRiesgoAIndividualRetorna422() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", individualId).header(KEY, "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionInmueble\":\"X\",\"arrendatario\":\"Y\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void agregarRiesgoAColectivaRetorna201() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", colectivaId).header(KEY, "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionInmueble\":\"X\",\"arrendatario\":\"Y\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void renovarYLuegoCancelar() throws Exception {
        mvc.perform(post("/polizas/{id}/renovar", individualId).header(KEY, "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"));
        mvc.perform(post("/polizas/{id}/cancelar", individualId).header(KEY, "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(post("/polizas/{id}/renovar", individualId).header(KEY, "123456"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void jsonMalFormadoRetorna400ConMensaje() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", colectivaId).header(KEY, "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la petición no es un JSON válido"))
                .andExpect(jsonPath("$.path").value("/polizas/" + colectivaId + "/riesgos"));
    }

    @Test
    void coreMockRegistraEvento() throws Exception {
        mvc.perform(post("/core-mock/evento").header(KEY, "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
                .andExpect(status().isAccepted());
    }
}
