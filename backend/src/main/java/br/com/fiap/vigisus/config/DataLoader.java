package br.com.fiap.vigisus.config;

import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.model.ServicoEspecializado;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.LeitoRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads demonstration data for the "dev" profile.
 * Idempotent — skips insertion when data already exists.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private static final String CO_IBGE_LAVRAS = "3131307";

    private static final String[][] HOSPITAIS = {
            // { coCnes, noFantasia, coMunicipio, lat, lon, telefone }
            {"2078767", "Hospital Regional Dr. Aloysio de Faria", "3131307",
                    "-21.254", "-44.994", "3583800"},
            {"2077051", "Santa Casa de Misericórdia de Lavras", "3131307",
                    "-21.245", "-45.001", "3835000"},
            {"2077108", "Hospital Unimed Sul de Minas Lavras", "3131307",
                    "-21.239", "-44.985", "3892500"},
    };

    private final MunicipioRepository municipioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final LeitoRepository leitoRepository;
    private final ServicoEspecializadoRepository servicoEspecializadoRepository;
    private final CasoDengueRepository casoDengueRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DataLoader] Iniciando carga de dados de demonstração...");
        carregarMunicipio();
        carregarHospitais();
        carregarCasosDengue();
        log.info("[DataLoader] Carga de dados de demonstração concluída.");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void carregarMunicipio() {
        if (municipioRepository.findByCoIbge(CO_IBGE_LAVRAS).isPresent()) {
            log.debug("[DataLoader] Município Lavras já existe. Pulando.");
            return;
        }

        Municipio lavras = Municipio.builder()
                .coIbge(CO_IBGE_LAVRAS)
                .noMunicipio("Lavras")
                .sgUf("MG")
                .nuLatitude(-21.245)
                .nuLongitude(-44.999)
                .populacao(102_000L)
                .updatedAt(LocalDateTime.now())
                .build();

        municipioRepository.save(lavras);
        log.info("[DataLoader] Município Lavras inserido (cod {}).", CO_IBGE_LAVRAS);
    }

    private void carregarHospitais() {
        for (String[] h : HOSPITAIS) {
            String coCnes = h[0];
            String noFantasia = h[1];
            String coMunicipio = h[2];
            double lat = Double.parseDouble(h[3]);
            double lon = Double.parseDouble(h[4]);
            String telefone = h[5];

            boolean estabelecimentoExiste = estabelecimentoRepository
                    .findByCoCnesIn(List.of(coCnes)).stream()
                    .anyMatch(e -> e.getCoCnes().equals(coCnes));

            if (!estabelecimentoExiste) {
                Estabelecimento est = Estabelecimento.builder()
                        .coCnes(coCnes)
                        .noFantasia(noFantasia)
                        .coMunicipio(coMunicipio)
                        .nuLatitude(lat)
                        .nuLongitude(lon)
                        .nuTelefone(telefone)
                        .tpGestao("E")
                        .competencia("202412")
                        .updatedAt(LocalDateTime.now())
                        .build();
                estabelecimentoRepository.save(est);
                log.info("[DataLoader] Estabelecimento inserido: {} (CNES {}).", noFantasia, coCnes);
            } else {
                log.debug("[DataLoader] Estabelecimento {} já existe. Pulando.", coCnes);
            }

            inserirLeitosSeNecessario(coCnes);
            inserirServicoSeNecessario(coCnes);
        }
    }

    private void inserirLeitosSeNecessario(String coCnes) {
        List<Leito> existentes = leitoRepository
                .findByCoCnesInAndTpLeitoAndQtSusGreaterThanEqual(List.of(coCnes), "74", 1);
        if (!existentes.isEmpty()) {
            log.debug("[DataLoader] Leitos para CNES {} já existem. Pulando.", coCnes);
            return;
        }

        // Leito clínico (tp 74)
        leitoRepository.save(Leito.builder()
                .coCnes(coCnes).tpLeito("74").dsLeito("LEITO CLÍNICO ADULTO")
                .qtExist(20).qtSus(15).competencia("202412").updatedAt(LocalDateTime.now())
                .build());

        // UTI adulto (tp 81)
        leitoRepository.save(Leito.builder()
                .coCnes(coCnes).tpLeito("81").dsLeito("UTI ADULTO TIPO II")
                .qtExist(10).qtSus(8).competencia("202412").updatedAt(LocalDateTime.now())
                .build());

        log.info("[DataLoader] Leitos inseridos para CNES {}.", coCnes);
    }

    private void inserirServicoSeNecessario(String coCnes) {
        // CNES service code 0135 = Infectologia
        boolean servicoExiste = servicoEspecializadoRepository
                .findDistinctCoCnesByServEspIn(List.of("0135"))
                .contains(coCnes);

        if (!servicoExiste) {
            servicoEspecializadoRepository.save(ServicoEspecializado.builder()
                    .coCnes(coCnes)
                    .servEsp("0135")
                    .classSr("01")
                    .competencia("202412")
                    .build());
            log.info("[DataLoader] Serviço de Infectologia inserido para CNES {}.", coCnes);
        } else {
            log.debug("[DataLoader] Serviço de Infectologia para CNES {} já existe. Pulando.", coCnes);
        }
    }

    private void carregarCasosDengue() {
        long countExistentes = casoDengueRepository
                .sumTotalCasosByCoMunicipioAndAno(CO_IBGE_LAVRAS, 2024);
        if (countExistentes > 0) {
            log.debug("[DataLoader] Casos de dengue 2024 já existem. Pulando anos já carregados.");
        }

        List<CasoDengue> novos = new ArrayList<>();

        for (int ano : new int[]{2023, 2024}) {
            long totalExistente = casoDengueRepository
                    .sumTotalCasosByCoMunicipioAndAno(CO_IBGE_LAVRAS, ano);
            if (totalExistente > 0) {
                log.debug("[DataLoader] Casos de dengue {} já existem. Pulando.", ano);
                continue;
            }

            for (int semana = 1; semana <= 52; semana++) {
                long casos = gerarCasosSazonais(semana);
                novos.add(CasoDengue.builder()
                        .coMunicipio(CO_IBGE_LAVRAS)
                        .ano(ano)
                        .semanaEpi(semana)
                        .totalCasos(casos)
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
        }

        if (!novos.isEmpty()) {
            casoDengueRepository.saveAll(novos);
            log.info("[DataLoader] {} registros de casos de dengue inseridos.", novos.size());
        }
    }

    /**
     * Generates a seasonal dengue case count for a given epidemiological week.
     * Peak season: weeks 5–15 (summer / rainy season in MG).
     *
     * @param semana epidemiological week (1–52)
     * @return simulated number of weekly cases
     */
    private long gerarCasosSazonais(int semana) {
        if (semana >= 5 && semana <= 15) {
            // Gaussian-like peak centred on week 10
            double peakDistance = Math.abs(semana - 10.0);
            return Math.round(400 - peakDistance * 30);
        }
        if (semana <= 4 || semana >= 48) {
            return 60;
        }
        return 20;
    }
}
