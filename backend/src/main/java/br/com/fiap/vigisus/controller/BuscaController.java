package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.BuscaRequest;
import br.com.fiap.vigisus.dto.BuscaResponse;
import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.exception.NotFoundException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.MunicipioService;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/busca")
@Tag(name = "Busca por Linguagem Natural")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BuscaController {

    private final IaService iaService;
    private final PerfilEpidemiologicoService perfilService;
    private final MunicipioService municipioService;

    @PostMapping
    @Operation(summary = "Interpreta pergunta em linguagem natural e retorna dados epidemiológicos com explicação em texto")
    public BuscaResponse buscar(@RequestBody BuscaRequest request) {

        // 1. Interpret question using IA
        IntencaoDTO intencao = iaService.interpretarPergunta(request.getPergunta());

        // 2. Find municipality by name within UF
        Municipio municipio = encontrarMunicipio(intencao.getMunicipio(), intencao.getUf());

        // 3. Get epidemiological profile
        int ano = intencao.getAno() != null ? intencao.getAno() : LocalDate.now().getYear();
        PerfilEpidemiologicoResponse perfil = perfilService.gerarPerfil(
                municipio.getCoIbge(), intencao.getDoenca(), ano);

        // 4. Generate AI text
        String textoIa = iaService.gerarTextoEpidemiologico(perfil);

        return BuscaResponse.builder()
                .interpretacao(intencao)
                .dados(perfil)
                .textoIa(textoIa)
                .build();
    }

    private Municipio encontrarMunicipio(String nome, String uf) {
        if (nome == null || nome.isBlank()) {
            throw new NotFoundException("Nome do município não identificado na pergunta");
        }
        if (uf != null && !uf.isBlank()) {
            List<Municipio> candidatos = municipioService.listarPorUf(uf);
            return candidatos.stream()
                    .filter(m -> m.getNoMunicipio().equalsIgnoreCase(nome))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException(
                            "Município não encontrado: " + nome + " - " + uf));
        }
        throw new NotFoundException("UF não identificada na pergunta");
    }
}
