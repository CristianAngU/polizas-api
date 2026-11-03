package com.cristianangulo.polizas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Póliza de arrendamiento. La prima = canon mensual x meses de vigencia.
 */
@Entity
@Table(name = "polizas")
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPoliza estado;

    /** Tomador: arrendatario (individual) o inmobiliaria/copropiedad (colectiva). */
    @Column(nullable = false)
    private String tomador;

    /** Beneficiario: arrendador. */
    @Column(nullable = false)
    private String beneficiario;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Integer mesesVigencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prima;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Riesgo> riesgos = new ArrayList<>();

    protected Poliza() { }

    public Poliza(String numero, TipoPoliza tipo, String tomador, String beneficiario,
                  LocalDate fechaInicio, Integer mesesVigencia, BigDecimal canonMensual) {
        this.numero = numero;
        this.tipo = tipo;
        this.tomador = tomador;
        this.beneficiario = beneficiario;
        this.fechaInicio = fechaInicio;
        this.mesesVigencia = mesesVigencia;
        this.fechaFin = fechaInicio.plusMonths(mesesVigencia);
        this.canonMensual = canonMensual.setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima();
        this.estado = EstadoPoliza.ACTIVA;
    }

    /** Prima = canon mensual x número de meses de vigencia. */
    private BigDecimal calcularPrima() {
        return canonMensual.multiply(BigDecimal.valueOf(mesesVigencia)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean esColectiva() { return tipo == TipoPoliza.COLECTIVA; }

    public boolean estaCancelada() { return estado == EstadoPoliza.CANCELADA; }

    public long riesgosActivos() {
        return riesgos.stream().filter(r -> r.getEstado() == EstadoRiesgo.ACTIVO).count();
    }

    /**
     * Renueva por el mismo periodo de vigencia, ajustando el canon por IPC y recalculando la prima.
     */
    public void renovar(BigDecimal ipc) {
        this.canonMensual = canonMensual.multiply(BigDecimal.ONE.add(ipc)).setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima();
        this.fechaInicio = this.fechaFin;
        this.fechaFin = this.fechaInicio.plusMonths(mesesVigencia);
        this.estado = EstadoPoliza.RENOVADA;
    }

    /** Cancela la póliza y todos sus riesgos. */
    public void cancelar() {
        this.estado = EstadoPoliza.CANCELADA;
        riesgos.forEach(Riesgo::cancelar);
    }

    public void agregarRiesgo(Riesgo riesgo) {
        riesgos.add(riesgo);
        riesgo.asignarPoliza(this);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public TipoPoliza getTipo() { return tipo; }
    public EstadoPoliza getEstado() { return estado; }
    public String getTomador() { return tomador; }
    public String getBeneficiario() { return beneficiario; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Integer getMesesVigencia() { return mesesVigencia; }
    public BigDecimal getCanonMensual() { return canonMensual; }
    public BigDecimal getPrima() { return prima; }
    public List<Riesgo> getRiesgos() { return riesgos; }
}
