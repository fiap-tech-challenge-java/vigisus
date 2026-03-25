package br.com.fiap.vigisus.application.operacional;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ConsultarProtocoloSurtoUseCase {

    public Map<String, Object> executar() {
        return Map.of(
                "titulo", "Protocolo de Surto Dengue - MS 2024",
                "passos", List.of(
                        "1. Notificar Vigilancia Epidemiologica Municipal",
                        "2. Registrar casos suspeitos no SINAN",
                        "3. Acionar Central de Regulacao para leitos de referencia",
                        "4. Solicitar nebulizacao a Secretaria de Saude",
                        "5. Ativar busca ativa nas areas de maior incidencia",
                        "6. Abrir sala de situacao com reuniao diaria"
                ),
                "contatos", Map.of(
                        "vigilancia_epidemiologica", "0800-644-6645",
                        "central_regulacao_mg", "(31) 3916-6868"
                )
        );
    }
}
