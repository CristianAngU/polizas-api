package com.cristianangulo.polizas.service;

import com.cristianangulo.polizas.domain.*;
import com.cristianangulo.polizas.dto.CrearRiesgoRequest;
import com.cristianangulo.polizas.exception.RecursoNoEncontradoException;
import com.cristianangulo.polizas.exception.ReglaNegocioException;
import com.cristianangulo.polizas.integration.CoreClient;
import com.cristianangulo.polizas.repository.PolizaRepository;
import com.cristianangulo.polizas.repository.RiesgoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PolizaService {

    static final String EVENTO_ACTUALIZACION = "ACTUALIZACION";

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreClient coreClient;
    private final BigDecimal ipc;

    public PolizaService(PolizaRepository polizaRepository, RiesgoRepository riesgoRepository,
                         CoreClient coreClient, @Value("${app.negocio.ipc}") BigDecimal ipc) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreClient = coreClient;
        this.ipc = ipc;
    }

    @Transactional(readOnly = true)
    public List<Poliza> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado);
    }

    @Transactional(readOnly = true)
    public List<Riesgo> listarRiesgos(Long polizaId) {
        obtenerPoliza(polizaId);
        return riesgoRepository.findByPolizaIdOrderById(polizaId);
    }

    /** Regla: no se puede renovar una póliza cancelada. Canon y prima se ajustan por IPC. */
    @Transactional
    public Poliza renovar(Long polizaId) {
        Poliza poliza = obtenerPoliza(polizaId);
        if (poliza.estaCancelada()) {
            throw new ReglaNegocioException("No se puede renovar una póliza cancelada");
        }
        poliza.renovar(ipc);
        coreClient.notificarEvento(EVENTO_ACTUALIZACION, poliza.getId());
        return poliza;
    }

    /** Regla: la cancelación de una póliza cancela todos sus riesgos. */
    @Transactional
    public Poliza cancelar(Long polizaId) {
        Poliza poliza = obtenerPoliza(polizaId);
        if (poliza.estaCancelada()) {
            throw new ReglaNegocioException("La póliza ya se encuentra cancelada");
        }
        poliza.cancelar();
        coreClient.notificarEvento(EVENTO_ACTUALIZACION, poliza.getId());
        return poliza;
    }

    /**
     * Reglas: solo pólizas colectivas admiten agregar riesgos por este endpoint;
     * una póliza individual solo puede tener 1 riesgo; no se agregan riesgos a pólizas canceladas.
     */
    @Transactional
    public Riesgo agregarRiesgo(Long polizaId, CrearRiesgoRequest request) {
        Poliza poliza = obtenerPoliza(polizaId);
        if (poliza.estaCancelada()) {
            throw new ReglaNegocioException("No se pueden agregar riesgos a una póliza cancelada");
        }
        if (!poliza.esColectiva()) {
            throw new ReglaNegocioException(
                    "Solo se pueden agregar riesgos a pólizas colectivas; una póliza individual solo puede tener 1 riesgo");
        }
        Riesgo riesgo = new Riesgo(request.direccionInmueble(), request.arrendatario());
        poliza.agregarRiesgo(riesgo);
        riesgoRepository.save(riesgo);
        coreClient.notificarEvento(EVENTO_ACTUALIZACION, poliza.getId());
        return riesgo;
    }

    @Transactional
    public Riesgo cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo con id " + riesgoId));
        if (riesgo.estaCancelado()) {
            throw new ReglaNegocioException("El riesgo ya se encuentra cancelado");
        }
        riesgo.cancelar();
        coreClient.notificarEvento(EVENTO_ACTUALIZACION, riesgo.getPoliza().getId());
        return riesgo;
    }

    private Poliza obtenerPoliza(Long polizaId) {
        return polizaRepository.findById(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la póliza con id " + polizaId));
    }
}
