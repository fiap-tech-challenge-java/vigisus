package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
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

    private final EncaminhamentoService encaminhamentoService;

    @GetMapping
    @Operation(
            summary = "Hospitais com estrutura compatível",
            description = """
                    Retorna hospitais ordenados por distância com leitos SUS
                    disponíveis e serviço de infectologia, baseado no CNES
                    (Cadastro Nacional de Estabelecimentos de Saúde).

                    IMPORTANTE: O CNES informa capacidade instalada (leitos que
                    o hospital possui). A confirmação de disponibilidade no momento
                    do encaminhamento deve ser feita por telefone — número fornecido
                    na resposta.

                    Retorna contexto informacional baseado em dados públicos do SUS.
                    Não realiza diagnóstico, triagem clínica nem define conduta médica.
                    A decisão de encaminhamento permanece com o profissional de saúde.
                    """)
    public EncaminhamentoResponse getEncaminhamento(
            @RequestParam String municipio,
            @RequestParam(defaultValue = "dengue") String condicao,
            @RequestParam(defaultValue = "moderada") String gravidade,
            @RequestParam(required = false) String tpLeito,
            @RequestParam(defaultValue = "1") int minLeitosSus) {

        String leito = (tpLeito != null && !tpLeito.isBlank())
                ? tpLeito
                : encaminhamentoService.resolverTpLeito(gravidade);
        return encaminhamentoService.buscarHospitais(municipio, leito, minLeitosSus);
    }
}
