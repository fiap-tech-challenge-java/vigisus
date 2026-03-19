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
    @Operation(summary = "Retorna hospitais mais próximos com estrutura adequada para a condição e gravidade informadas")
    public EncaminhamentoResponse getEncaminhamento(
            @RequestParam String municipio,
            @RequestParam(defaultValue = "dengue") String condicao,
            @RequestParam(defaultValue = "moderada") String gravidade) {

        return encaminhamentoService.buscarHospitais(municipio, condicao, gravidade);
    }
}
