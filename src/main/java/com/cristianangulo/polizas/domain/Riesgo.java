package com.cristianangulo.polizas.domain;

import jakarta.persistence.*;

/**
 * Riesgo asegurado: el inmueble arrendado y su arrendatario.
 */
@Entity
@Table(name = "riesgos")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String direccionInmueble;

    @Column(nullable = false)
    private String arrendatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRiesgo estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    protected Riesgo() { }

    public Riesgo(String direccionInmueble, String arrendatario) {
        this.direccionInmueble = direccionInmueble;
        this.arrendatario = arrendatario;
        this.estado = EstadoRiesgo.ACTIVO;
    }

    void asignarPoliza(Poliza poliza) { this.poliza = poliza; }

    public void cancelar() { this.estado = EstadoRiesgo.CANCELADO; }

    public boolean estaCancelado() { return estado == EstadoRiesgo.CANCELADO; }

    public Long getId() { return id; }
    public String getDireccionInmueble() { return direccionInmueble; }
    public String getArrendatario() { return arrendatario; }
    public EstadoRiesgo getEstado() { return estado; }
    public Poliza getPoliza() { return poliza; }
}
