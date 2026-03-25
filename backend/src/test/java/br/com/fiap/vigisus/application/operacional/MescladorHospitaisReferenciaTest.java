package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MescladorHospitaisReferenciaTest {

    private final MescladorHospitaisReferencia mesclador = new MescladorHospitaisReferencia();

    @Test
    void mesclar_removeDuplicadosOrdenaELimita() {
        HospitalDTO h1 = HospitalDTO.builder().coCnes("1").noFantasia("H1").distanciaKm(15.0).build();
        HospitalDTO h2 = HospitalDTO.builder().coCnes("2").noFantasia("H2").distanciaKm(5.0).build();
        HospitalDTO h3 = HospitalDTO.builder().coCnes("3").noFantasia("H3").distanciaKm(25.0).build();
        HospitalDTO h4 = HospitalDTO.builder().coCnes("4").noFantasia("H4").distanciaKm(35.0).build();

        List<HospitalDTO> resultado = mesclador.mesclar(
                List.of(h1, h2),
                List.of(h2, h3, h4),
                3
        );

        assertThat(resultado).extracting(HospitalDTO::getCoCnes).containsExactly("2", "1", "3");
    }
}
