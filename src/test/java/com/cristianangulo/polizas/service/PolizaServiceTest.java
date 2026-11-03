package com.cristianangulo.polizas.service;

import com.cristianangulo.polizas.domain.*;
import com.cristianangulo.polizas.dto.CrearRiesgoRequest;
import com.cristianangulo.polizas.exception.RecursoNoEncontradoException;
import com.cristianangulo.polizas.exception.ReglaNegocioException;
import com.cristianangulo.polizas.integration.CoreClient;
import com.cristianangulo.polizas.repository.PolizaRepository;
import com.cristianangulo.polizas.repository.RiesgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PolizaServiceTest {

    private PolizaRepository polizaRepository;
    private RiesgoRepository riesgoRepository;
    private CoreClient coreClient;
    private PolizaService service;

    @BeforeEach
    void setUp() {
        polizaRepository = mock(PolizaRepository.class);
        riesgoRepository = mock(RiesgoRepository.class);
        coreClient = mock(CoreClient.class);
        service = new PolizaService(polizaRepository, riesgoRepository, coreClient, new BigDecimal("0.10"));
    }

    private Poliza poliza(TipoPoliza tipo) {
        return new Poliza("P-1", tipo, "Tomador", "Beneficiario", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
    }

    @Test
    void renovarAjustaCanonYPrimaPorIpcYCambiaEstado() {
        Poliza p = poliza(TipoPoliza.INDIVIDUAL);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(p));

        Poliza renovada = service.renovar(1L);

        assertThat(renovada.getCanonMensual()).isEqualByComparingTo("1100000.00");
        assertThat(renovada.getPrima()).isEqualByComparingTo("13200000.00");
        assertThat(renovada.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
        assertThat(renovada.getFechaInicio()).isEqualTo(LocalDate.of(2027, 1, 1));
        verify(coreClient).notificarEvento("ACTUALIZACION", p.getId());
    }

    @Test
    void noSePuedeRenovarUnaPolizaCancelada() {
        Poliza p = poliza(TipoPoliza.INDIVIDUAL);
        p.cancelar();
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.renovar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cancelada");
        verifyNoInteractions(coreClient);
    }

    @Test
    void cancelarPolizaCancelaTodosSusRiesgos() {
        Poliza p = poliza(TipoPoliza.COLECTIVA);
        p.agregarRiesgo(new Riesgo("Dir 1", "Arrendatario 1"));
        p.agregarRiesgo(new Riesgo("Dir 2", "Arrendatario 2"));
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(p));

        service.cancelar(1L);

        assertThat(p.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(p.getRiesgos()).allMatch(Riesgo::estaCancelado);
        verify(coreClient).notificarEvento("ACTUALIZACION", p.getId());
    }

    @Test
    void cancelarRiesgoLoMarcaCanceladoYNotificaAlCore() {
        Poliza p = poliza(TipoPoliza.INDIVIDUAL);
        Riesgo riesgo = new Riesgo("Dir", "A");
        p.agregarRiesgo(riesgo);
        when(riesgoRepository.findById(5L)).thenReturn(Optional.of(riesgo));

        Riesgo cancelado = service.cancelarRiesgo(5L);

        assertThat(cancelado.estaCancelado()).isTrue();
        verify(coreClient).notificarEvento("ACTUALIZACION", p.getId());
    }

    @Test
    void noSePuedeCancelarUnRiesgoYaCancelado() {
        Riesgo riesgo = new Riesgo("Dir", "A");
        riesgo.cancelar();
        when(riesgoRepository.findById(5L)).thenReturn(Optional.of(riesgo));

        assertThatThrownBy(() -> service.cancelarRiesgo(5L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cancelado");
        verifyNoInteractions(coreClient);
    }

    @Test
    void riesgoInexistenteLanzaNoEncontrado() {
        when(riesgoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelarRiesgo(9L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9");
        verifyNoInteractions(coreClient);
    }

    @Test
    void noSePuedenAgregarRiesgosAPolizaIndividual() {
        Poliza p = poliza(TipoPoliza.INDIVIDUAL);
        p.agregarRiesgo(new Riesgo("Dir 1", "Arrendatario 1"));
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.agregarRiesgo(1L, new CrearRiesgoRequest("Dir 2", "Otro")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("colectivas");
        assertThat(p.getRiesgos()).hasSize(1);
    }

    @Test
    void agregarRiesgoAPolizaColectiva() {
        Poliza p = poliza(TipoPoliza.COLECTIVA);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(p));

        Riesgo r = service.agregarRiesgo(1L, new CrearRiesgoRequest("Calle 1", "Ana"));

        assertThat(p.getRiesgos()).containsExactly(r);
        verify(riesgoRepository).save(r);
        verify(coreClient).notificarEvento("ACTUALIZACION", p.getId());
    }
}
