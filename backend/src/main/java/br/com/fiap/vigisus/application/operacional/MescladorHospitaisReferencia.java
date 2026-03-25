package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MescladorHospitaisReferencia {

    public List<HospitalDTO> mesclar(List<HospitalDTO> clinicos, List<HospitalDTO> uti, int limite) {
        Map<String, HospitalDTO> merged = new LinkedHashMap<>();

        if (clinicos != null) {
            for (HospitalDTO hospital : clinicos) {
                merged.put(hospital.getCoCnes(), hospital);
            }
        }

        if (uti != null) {
            for (HospitalDTO hospital : uti) {
                merged.putIfAbsent(hospital.getCoCnes(), hospital);
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparingDouble(HospitalDTO::getDistanciaKm))
                .limit(limite)
                .toList();
    }
}
