package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.encaminhamento.ConsultarEncaminhamentoUseCase;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/encaminhar")
@Tag(name = "Encaminhamento de Pacientes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EncaminhamentoController {

    private final ConsultarEncaminhamentoUseCase consultarEncaminhamentoUseCase;

    @GetMapping
    @Operation(
            summary = "Hospitais com estrutura compativel",
            description = """
                    Retorna hospitais ordenados por distancia com leitos SUS
                    disponiveis e servico de infectologia, baseado no CNES
                    (Cadastro Nacional de Estabelecimentos de Saude).

                    IMPORTANTE: O CNES informa capacidade instalada (leitos que
                    o hospital possui). A confirmacao de disponibilidade no momento
                    do encaminhamento deve ser feita por telefone - numero fornecido
                    na resposta.

                    Retorna contexto informacional baseado em dados publicos do SUS.
                    Nao realiza diagnostico, triagem clinica nem define conduta medica.
                    A decisao de encaminhamento permanece com o profissional de saude.
                    """)
    public EncaminhamentoResponse getEncaminhamento(
            @RequestParam String municipio,
            @RequestParam(defaultValue = "dengue") String condicao,
            @RequestParam(defaultValue = "moderada") String gravidade,
            @RequestParam(required = false) String tpLeito,
            @RequestParam(defaultValue = "1") int minLeitosSus) {
        return consultarEncaminhamentoUseCase.executar(municipio, condicao, gravidade, tpLeito, minLeitosSus);
    }
}
