package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.encaminhamento.ConsultarHospitaisCapitaisUseCase;
import br.com.fiap.vigisus.application.encaminhamento.ConsultarHospitaisUseCase;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
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

    private final ConsultarHospitaisUseCase consultarHospitaisUseCase;
    private final ConsultarHospitaisCapitaisUseCase consultarHospitaisCapitaisUseCase;

    @GetMapping
    @Operation(summary = "Lista hospitais num raio informado com o servico solicitado")
    public List<EncaminhamentoResponse.HospitalDTO> getHospitais(
            @RequestParam String municipio,
            @RequestParam(defaultValue = "74") String tpLeito,
            @RequestParam(defaultValue = "1") int minLeitosSus) {
        return consultarHospitaisUseCase.executar(municipio, tpLeito, minLeitosSus);
    }

    @GetMapping("/capitais")
    @Operation(summary = "Lista principais hospitais das capitais estaduais")
    public List<EncaminhamentoResponse.HospitalDTO> getHospitaisCapitais(
            @RequestParam(required = false) String uf) {
        return consultarHospitaisCapitaisUseCase.executar(uf);
    }
}
