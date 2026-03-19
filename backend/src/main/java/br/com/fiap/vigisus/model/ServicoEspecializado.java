package br.com.fiap.vigisus.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "servicos_especializados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoEspecializado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "co_cnes")
    private String coCnes;

    @Column(name = "serv_esp")
    private String servEsp;

    @Column(name = "class_sr")
    private String classSr;

    @Column(name = "competencia")
    private String competencia;
}
