package br.com.fiap.vigisus.dto;

import br.com.fiap.vigisus.model.Estabelecimento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardBrasilResponse {

    private BrasilEpidemiologicoResponse brasil;
    private PrevisaoRiscoResponse risco;
    private List<Estabelecimento> hospitais;
}
