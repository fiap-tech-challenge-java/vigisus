package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadoHistoricoServiceTest {

    @Mock
    private CasoDengueRepository casoDengueRepository;

    private EstadoHistoricoService service;

    @BeforeEach
    void setUp() {
        service = new EstadoHistoricoService(casoDengueRepository);
    }

    @Test
    void gerarPerfilEstado_normalizaUfECalculaTendencia() {
        when(casoDengueRepository.agregaTotaisEstadoNoAno("MG", 2024)).thenReturn(List.<Object[]>of(new Object[]{450L, 100_000L}));
        when(casoDengueRepository.agregaSemanasPorEstado("MG", 2024)).thenReturn(List.<Object[]>of(
                new Object[]{1, 10L}, new Object[]{2, 10L}, new Object[]{3, 10L}, new Object[]{4, 10L},
                new Object[]{5, 20L}, new Object[]{6, 20L}, new Object[]{7, 20L}, new Object[]{8, 20L}
        ));
        when(casoDengueRepository.agregaSemanasPorEstado("MG", 2023)).thenReturn(List.<Object[]>of(
                new Object[]{1, 5L}, new Object[]{2, 6L}
        ));

        PerfilEpidemiologicoResponse response = service.gerarPerfilEstado(" mg ", "dengue", 2024);

        assertThat(response.getCoIbge()).isEqualTo("MG");
        assertThat(response.getMunicipio()).isEqualTo("Estado MG");
        assertThat(response.getTotal()).isEqualTo(450L);
        assertThat(response.getIncidencia()).isCloseTo(450.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(response.getClassificacao()).isEqualTo("EPIDEMIA");
        assertThat(response.getTendencia()).isEqualTo("CRESCENTE");
        assertThat(response.getSemanas()).hasSize(8);
        assertThat(response.getSemanasAnoAnterior()).hasSize(2);
    }

    @Test
    void gerarPerfilEstado_semDadosMantemValoresPadrao() {
        when(casoDengueRepository.agregaTotaisEstadoNoAno("RJ", 2024)).thenReturn(List.of());
        when(casoDengueRepository.agregaSemanasPorEstado("RJ", 2024)).thenReturn(List.<Object[]>of(
                new Object[]{1, 0L}, new Object[]{2, 0L}
        ));
        when(casoDengueRepository.agregaSemanasPorEstado("RJ", 2023)).thenReturn(List.of());

        PerfilEpidemiologicoResponse response = service.gerarPerfilEstado("rj", "dengue", 2024);

        assertThat(response.getTotal()).isZero();
        assertThat(response.getIncidencia()).isZero();
        assertThat(response.getClassificacao()).isEqualTo("BAIXO");
        assertThat(response.getTendencia()).isEqualTo("ESTAVEL");
    }
}
