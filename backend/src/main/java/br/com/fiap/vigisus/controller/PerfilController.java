package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/perfil")
@Tag(name = "Perfil Epidemiológico")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilEpidemiologicoService perfilService;
    private final IaService iaService;

    @GetMapping("/{coIbge}")
    @Operation(summary = "Retorna o perfil epidemiológico completo de um município")
    public PerfilEpidemiologicoResponse getPerfil(
            @PathVariable String coIbge,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {

        if (ano == null) {
            ano = LocalDate.now().getYear();
        }

        PerfilEpidemiologicoResponse perfil = perfilService.gerarPerfil(coIbge, doenca, ano);
        perfil.setTextoIa(iaService.gerarTextoEpidemiologico(perfil));
        return perfil;
    }
}
