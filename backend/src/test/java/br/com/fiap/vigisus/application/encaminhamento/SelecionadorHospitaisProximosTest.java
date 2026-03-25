package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.domain.geografia.CalculadoraDistanciaGeografica;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.ServicoEspecializado;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SelecionadorHospitaisProximosTest {

    private final SelecionadorHospitaisProximos selecionador =
            new SelecionadorHospitaisProximos(new CalculadoraDistanciaGeografica());

    @Test
    void selecionar_filtraPorRaioOrdenaEDestacaInfectologia() {
        Leito proximo = Leito.builder().coCnes("1").qtSus(5).build();
        Leito distante = Leito.builder().coCnes("2").qtSus(2).build();

        Estabelecimento hospitalProximo = Estabelecimento.builder()
                .coCnes("1")
                .noFantasia("Hospital Perto")
                .coMunicipio("3131307")
                .nuLatitude(-21.260)
                .nuLongitude(-45.010)
                .build();
        Estabelecimento hospitalDistante = Estabelecimento.builder()
                .coCnes("2")
                .noFantasia("Hospital Longe")
                .coMunicipio("3170909")
                .nuLatitude(-21.900)
                .nuLongitude(-45.200)
                .build();

        ServicoEspecializado infectologia = ServicoEspecializado.builder()
                .coCnes("1")
                .servEsp("135")
                .build();

        List<HospitalDTO> resultado = selecionador.selecionar(
                -21.245,
                -44.999,
                List.of(proximo, distante),
                Map.of("1", hospitalProximo, "2", hospitalDistante),
                Map.of("1", List.of(infectologia)),
                50,
                5
        );

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNoFantasia()).isEqualTo("Hospital Perto");
        assertThat(resultado.get(0).isServicoInfectologia()).isTrue();
    }

    @Test
    void selecionar_ignoraEstabelecimentoSemCoordenadas() {
        Leito leito = Leito.builder().coCnes("1").qtSus(5).build();
        Estabelecimento semCoordenadas = Estabelecimento.builder()
                .coCnes("1")
                .noFantasia("Hospital Sem Coordenadas")
                .build();

        List<HospitalDTO> resultado = selecionador.selecionar(
                -21.245,
                -44.999,
                List.of(leito),
                Map.of("1", semCoordenadas),
                Map.of(),
                50,
                5
        );

        assertThat(resultado).isEmpty();
    }
}
