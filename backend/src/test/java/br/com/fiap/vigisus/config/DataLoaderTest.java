package br.com.fiap.vigisus.config;

import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.LeitoRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataLoaderTest {

    private MunicipioRepository municipioRepository;
    private EstabelecimentoRepository estabelecimentoRepository;
    private LeitoRepository leitoRepository;
    private ServicoEspecializadoRepository servicoEspecializadoRepository;
    private CasoDengueRepository casoDengueRepository;
    private DataLoader dataLoader;

    @BeforeEach
    void setUp() {
        municipioRepository = mock(MunicipioRepository.class);
        estabelecimentoRepository = mock(EstabelecimentoRepository.class);
        leitoRepository = mock(LeitoRepository.class);
        servicoEspecializadoRepository = mock(ServicoEspecializadoRepository.class);
        casoDengueRepository = mock(CasoDengueRepository.class);
        dataLoader = new DataLoader(
                municipioRepository,
                estabelecimentoRepository,
                leitoRepository,
                servicoEspecializadoRepository,
                casoDengueRepository);
    }

    @Test
    void run_carregaDadosQuandoNaoExistem() {
        when(municipioRepository.findByCoIbge("3131307")).thenReturn(Optional.empty());
        when(estabelecimentoRepository.findByCoCnesIn(any())).thenReturn(List.of());
        when(leitoRepository.findByCoCnesInAndTpLeitoAndQtSusGreaterThanEqual(any(), any(), any(Integer.class))).thenReturn(List.of());
        when(servicoEspecializadoRepository.findDistinctCoCnesByServEspIn(any())).thenReturn(Set.of());
        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno("3131307", 2024)).thenReturn(0L);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno("3131307", 2023)).thenReturn(0L);

        dataLoader.run(new DefaultApplicationArguments(new String[]{}));

        verify(municipioRepository).save(any(Municipio.class));
        verify(estabelecimentoRepository, org.mockito.Mockito.times(3)).save(any(Estabelecimento.class));
        verify(leitoRepository, org.mockito.Mockito.times(6)).save(any(Leito.class));
        verify(servicoEspecializadoRepository, org.mockito.Mockito.times(3)).save(any());

        ArgumentCaptor<List<CasoDengue>> captor = ArgumentCaptor.forClass(List.class);
        verify(casoDengueRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(104);
    }

    @Test
    void run_pulaCargaQuandoDadosJaExistem() {
        when(municipioRepository.findByCoIbge("3131307")).thenReturn(Optional.of(Municipio.builder().coIbge("3131307").build()));
        when(estabelecimentoRepository.findByCoCnesIn(any())).thenReturn(List.of(Estabelecimento.builder().coCnes("2078767").build()));
        when(leitoRepository.findByCoCnesInAndTpLeitoAndQtSusGreaterThanEqual(any(), any(), any(Integer.class)))
                .thenReturn(List.of(Leito.builder().coCnes("2078767").build()));
        when(servicoEspecializadoRepository.findDistinctCoCnesByServEspIn(any())).thenReturn(Set.of("2078767", "2077051", "2077108"));
        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno("3131307", 2024)).thenReturn(10L);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno("3131307", 2023)).thenReturn(10L);

        dataLoader.run(new DefaultApplicationArguments(new String[]{}));

        verify(municipioRepository, never()).save(any(Municipio.class));
        verify(casoDengueRepository, never()).saveAll(any());
    }
}
