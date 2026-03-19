package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {

    private String uf;
    private String doenca;
    private int ano;
    private int totalMunicipiosComDados;
    private List<RankingMunicipioDTO> ranking;
}
