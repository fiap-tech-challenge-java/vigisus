package br.com.fiap.vigisus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "estabelecimentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "co_cnes", unique = true)
    private String coCnes;

    @Column(name = "no_fantasia")
    private String noFantasia;

    @Column(name = "co_municipio")
    private String coMunicipio;

    @Column(name = "nu_latitude")
    private Double nuLatitude;

    @Column(name = "nu_longitude")
    private Double nuLongitude;

    @Column(name = "nu_telefone")
    private String nuTelefone;

    @Column(name = "tp_gestao")
    private String tpGestao;

    @Column(name = "competencia")
    private String competencia;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
