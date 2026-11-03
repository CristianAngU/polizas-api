package com.cristianangulo.polizas.config;

import com.cristianangulo.polizas.domain.Poliza;
import com.cristianangulo.polizas.domain.Riesgo;
import com.cristianangulo.polizas.domain.TipoPoliza;
import com.cristianangulo.polizas.repository.PolizaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de ejemplo para probar la API (no se cargan en el perfil de pruebas). */
@Configuration
@Profile("!test")
public class DataLoader {

    @Bean
    CommandLineRunner cargarDatos(PolizaRepository repo) {
        return args -> {
            Poliza individual = new Poliza("IND-0001", TipoPoliza.INDIVIDUAL, "Laura Gómez", "Carlos Pérez",
                    LocalDate.of(2026, 1, 1), 12, new BigDecimal("1500000"));
            individual.agregarRiesgo(new Riesgo("Calle 100 # 15-20, Apto 301", "Laura Gómez"));

            Poliza colectiva = new Poliza("COL-0001", TipoPoliza.COLECTIVA, "Inmobiliaria Andes S.A.S.",
                    "Propietarios Conjunto Los Pinos", LocalDate.of(2026, 3, 1), 12, new BigDecimal("2000000"));
            colectiva.agregarRiesgo(new Riesgo("Carrera 7 # 80-10, Apto 101", "Andrés Ruiz"));
            colectiva.agregarRiesgo(new Riesgo("Carrera 7 # 80-10, Apto 202", "María López"));

            Poliza individual2 = new Poliza("IND-0002", TipoPoliza.INDIVIDUAL, "Jorge Díaz", "Ana Torres",
                    LocalDate.of(2026, 6, 1), 6, new BigDecimal("1200000"));

            repo.save(individual);
            repo.save(colectiva);
            repo.save(individual2);
        };
    }
}
