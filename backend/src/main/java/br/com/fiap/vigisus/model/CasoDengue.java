package br.com.fiap.vigisus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "casos_dengue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasoDengue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "co_municipio")
    private String coMunicipio;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "semana_epi")
    private Integer semanaEpi;

    @Column(name = "total_casos")
    private Long totalCasos;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
