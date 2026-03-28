package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.operacional.ConstruirContextoEpidemiologicoOperacional;
import br.com.fiap.vigisus.application.operacional.MescladorHospitaisReferencia;
import br.com.fiap.vigisus.application.operacional.MontadorContextoOperacional;
import br.com.fiap.vigisus.application.operacional.MontadorPrevisaoOperacional;
import br.com.fiap.vigisus.domain.encaminhamento.TipoLeito;
import br.com.fiap.vigisus.domain.geografia.CoIbge;
import br.com.fiap.vigisus.domain.operacional.CalculadoraNivelAtencaoOperacional;
import br.com.fiap.vigisus.domain.operacional.ChecklistOperacionalPolicy;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.ContextoEpidemiologicoDTO;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.PrevisaoProximosDiasDTO;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Camada de orquestração que coordena use cases e integrações externas.
 *
 * <p>Depende exclusivamente de Ports (application/port/) — nunca de repositórios
 * JPA diretamente — respeitando a regra de dependência da Clean Architecture.
 *
 * <p>Candidato à migração para use case dedicado na versão 2.0.
 */
@Service
@RequiredArgsConstructor
public class PressaoOperacionalService {

    private final MunicipioService municipioService;
    private final PrevisaoRiscoService previsaoRiscoService;
    private final EncaminhamentoService encaminhamentoService;
    private final IaService iaService;
    private final CalculadoraNivelAtencaoOperacional calculadoraNivelAtencaoOperacional;
    private final MontadorPrevisaoOperacional montadorPrevisaoOperacional;
    private final MontadorContextoOperacional montadorContextoOperacional;
    private final ChecklistOperacionalPolicy checklistOperacionalPolicy;
    private final MescladorHospitaisReferencia mescladorHospitaisReferencia;
    private final ConstruirContextoEpidemiologicoOperacional construirContextoEpidemiologicoOperacional;

    public PressaoOperacionalResponse avaliarPressao(PressaoOperacionalRequest req) {
        CoIbge coIbge = CoIbge.of(req.getMunicipio());
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge.value());

        ContextoEpidemiologicoDTO contexto = construirContexto(coIbge);

        PrevisaoRiscoResponse riscoClimatico = calcularRiscoComFallback(coIbge.value());
        String nivelAtencao = calcularNivelAtencao(
                req.getSuspeitasDengueDia(),
                contexto.getClassificacaoAtual(),
                contexto.getTendencia(),
                riscoClimatico
        );

        PrevisaoProximosDiasDTO previsao = construirPrevisao(riscoClimatico);

        String contextoAtual = montarContextoAtual(
                req.getSuspeitasDengueDia(),
                municipio.getNoMunicipio(),
                contexto.getClassificacaoAtual(),
                contexto.getTendencia()
        );
        String padraoHistorico = montarPadraoHistorico(contexto.getComparativoHistorico());

        List<String> checklistInformativo = montarChecklistInformativo(nivelAtencao, contexto, previsao);
        List<HospitalDTO> hospitaisReferencia = buscarHospitaisReferencia(coIbge);

        String contextoTexto = montadorContextoOperacional.montarContextoIa(
                contextoAtual,
                padraoHistorico,
                nivelAtencao
        );
        String textoIa = iaService.gerarTextoOperacional(contextoTexto);

        return PressaoOperacionalResponse.builder()
                .municipio(coIbge.value())
                .tipoUnidade(req.getTipoUnidade())
                .nivelAtencao(nivelAtencao)
                .contextoAtual(contextoAtual)
                .padraoHistorico(padraoHistorico)
                .checklistInformativo(checklistInformativo)
                .contexto(contexto)
                .previsao(previsao)
                .hospitaisReferencia(hospitaisReferencia)
                .textoIa(textoIa)
                .build();
    }

    ContextoEpidemiologicoDTO construirContexto(String coIbge) {
        return construirContexto(CoIbge.of(coIbge));
    }

    ContextoEpidemiologicoDTO construirContexto(CoIbge coIbge) {
        return construirContextoEpidemiologicoOperacional.executar(coIbge);
    }

    String calcularNivelAtencao(int suspeitasDia, String classificacao, String tendencia, PrevisaoRiscoResponse risco) {
        return calculadoraNivelAtencaoOperacional.calcular(
                suspeitasDia,
                classificacao,
                tendencia,
                risco != null ? risco.getClassificacao() : null
        );
    }

    List<HospitalDTO> buscarHospitaisReferencia(String coIbge) {
        return buscarHospitaisReferencia(CoIbge.of(coIbge));
    }

    List<HospitalDTO> buscarHospitaisReferencia(CoIbge coIbge) {
        EncaminhamentoResponse clinicos = encaminhamentoService.buscarHospitais(
                coIbge.value(),
                TipoLeito.clinico().codigo(),
                10
        );
        EncaminhamentoResponse uti = encaminhamentoService.buscarHospitais(
                coIbge.value(),
                TipoLeito.uti().codigo(),
                5
        );
        return mescladorHospitaisReferencia.mesclar(clinicos.getHospitais(), uti.getHospitais(), 3);
    }

    private PrevisaoRiscoResponse calcularRiscoComFallback(String coIbge) {
        try {
            return previsaoRiscoService.calcularRisco(coIbge);
        } catch (Exception e) {
            return null;
        }
    }

    private PrevisaoProximosDiasDTO construirPrevisao(PrevisaoRiscoResponse risco) {
        return montadorPrevisaoOperacional.montar(risco);
    }

    private String montarContextoAtual(int suspeitasDia, String nomeMunicipio, String classificacao, String tendencia) {
        return montadorContextoOperacional.montarContextoAtual(suspeitasDia, nomeMunicipio, classificacao, tendencia);
    }

    private String montarPadraoHistorico(String comparativoHistorico) {
        return montadorContextoOperacional.montarPadraoHistorico(comparativoHistorico);
    }

    private List<String> montarChecklistInformativo(String nivelAtencao,
                                                    ContextoEpidemiologicoDTO contexto,
                                                    PrevisaoProximosDiasDTO previsao) {
        return checklistOperacionalPolicy.montarChecklist(nivelAtencao, contexto, previsao);
    }
}
