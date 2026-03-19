package br.com.fiap.vigisus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leitos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Leito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "co_cnes")
    private String coCnes;

    @Column(name = "tp_leito")
    private String tpLeito;

    @Column(name = "ds_leito")
    private String dsLeito;

    @Column(name = "qt_exist")
    private Integer qtExist;

    @Column(name = "qt_sus")
    private Integer qtSus;

    @Column(name = "competencia")
    private String competencia;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
