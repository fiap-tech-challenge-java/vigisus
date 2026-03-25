package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.exception.DadosInsuficientesException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrasilEpidemiologicoServiceTest {

    @Mock
    private MunicipioRepository municipioRepository;

    @Mock
    private CasoDengueRepository casoDengueRepository;

    @Mock
    private EncaminhamentoService encaminhamentoService;

    private BrasilEpidemiologicoService service;

    @BeforeEach
    void setUp() {
        service = new BrasilEpidemiologicoService(municipioRepository, casoDengueRepository, encaminhamentoService);
    }

    @Test
    void gerarPerfilBrasil_retornaAgregacaoCompleta() {
        when(casoDengueRepository.agregaCasosPorEstadoNoAno(2024)).thenReturn(List.of(
                new Object[]{"MG", 1_000L, 100_000L},
                new Object[]{"SP", 500L, 200_000L}
        ));
        when(casoDengueRepository.agregaSemanasBrasil(2024)).thenReturn(List.of(
                new Object[]{1, 10L}, new Object[]{2, 10L}, new Object[]{3, 10L}, new Object[]{4, 10L},
                new Object[]{5, 20L}, new Object[]{6, 20L}, new Object[]{7, 20L}, new Object[]{8, 20L}
        ));
        when(casoDengueRepository.agregaSemanasBrasil(2023)).thenReturn(List.of(
                new Object[]{1, 8L}, new Object[]{2, 8L}, new Object[]{3, 8L}, new Object[]{4, 8L}
        ));
        when(casoDengueRepository.agregaCasosPorMunicipioNoAno(2024)).thenReturn(List.of(
                new Object[]{"3106200", 600L},
                new Object[]{"3550308", 400L},
                new Object[]{"9999999", 300L}
        ));
        when(municipioRepository.findByCoIbge("3106200")).thenReturn(Optional.of(Municipio.builder()
                .coIbge("3106200")
                .noMunicipio("Belo Horizonte")
                .sgUf("MG")
                .populacao(100_000L)
                .build()));
        when(municipioRepository.findByCoIbge("3550308")).thenReturn(Optional.of(Municipio.builder()
                .coIbge("3550308")
                .noMunicipio("Sao Paulo")
                .sgUf("SP")
                .populacao(200_000L)
                .build()));
        when(municipioRepository.findByCoIbge("9999999")).thenReturn(Optional.empty());
        when(encaminhamentoService.buscarHospitaisDasCapitais(null)).thenReturn(List.of(
                EncaminhamentoResponse.HospitalDTO.builder().coCnes("1").noFantasia("Hospital Central").build()
        ));

        BrasilEpidemiologicoResponse response = service.gerarPerfilBrasil("dengue", 2024);

        assertThat(response.getRegiao()).isEqualTo("Brasil");
        assertThat(response.getAno()).isEqualTo(2024);
        assertThat(response.getTotalCasos()).isEqualTo(1_500L);
        assertThat(response.getIncidencia()).isEqualTo(500.0);
        assertThat(response.getClassificacao()).isEqualTo("EPIDEMIA");
        assertThat(response.getTendencia()).isEqualTo("CRESCENTE");
        assertThat(response.getEstadosPiores()).hasSize(2);
        assertThat(response.getEstadosPiores().get(0).getSgUf()).isEqualTo("MG");
        assertThat(response.getEstadosPiores().get(0).getPosicao()).isEqualTo(1);
        assertThat(response.getMunicipiosPiores()).hasSize(2);
        assertThat(response.getMunicipiosPiores().get(0).getMunicipio()).isEqualTo("Belo Horizonte");
        assertThat(response.getMunicipiosPiores().get(0).getPosicao()).isEqualTo(1);
        assertThat(response.getCasosPerEstado()).containsEntry("MG", 1_000L);
        assertThat(response.getHospitais()).hasSize(1);
    }

    @Test
    void gerarPerfilBrasil_lancaQuandoNaoHaDados() {
        when(casoDengueRepository.agregaCasosPorEstadoNoAno(2024)).thenReturn(List.of());

        assertThatThrownBy(() -> service.gerarPerfilBrasil("dengue", 2024))
                .isInstanceOf(DadosInsuficientesException.class)
                .hasMessageContaining("Brasil");
    }
}
