package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.HospitalDTO;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hospitais")
@Tag(name = "Hospitais")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HospitalController {

    private final EncaminhamentoService encaminhamentoService;

    @GetMapping
    @Operation(summary = "Lista hospitais num raio informado com o serviço solicitado")
    public List<HospitalDTO> getHospitais(
            @RequestParam String municipio,
            @RequestParam(required = false) String servico,
            @RequestParam(defaultValue = "300") Integer raioKm,
            @RequestParam(defaultValue = "74") String tpLeito) {

        return encaminhamentoService.buscarHospitaisPorRaio(municipio, servico, raioKm, tpLeito);
    }
}
