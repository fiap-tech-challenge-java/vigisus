package br.com.fiap.vigisus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "municipios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "co_ibge", unique = true)
    private String coIbge;

    @Column(name = "no_municipio")
    private String noMunicipio;

    @Column(name = "sg_uf")
    private String sgUf;

    @Column(name = "nu_latitude")
    private Double nuLatitude;

    @Column(name = "nu_longitude")
    private Double nuLongitude;

    @Column(name = "populacao")
    private Long populacao;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
