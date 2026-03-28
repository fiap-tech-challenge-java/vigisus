package br.com.fiap.vigisus;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class VigisusApplication {

    public static void main(String[] args) {
        SpringApplication.run(VigisusApplication.class, args);
    }

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("VigiSUS API")
                .description("""
                    Plataforma de vigilância epidemiológica do SUS.
                    Integra dados abertos do DATASUS (SINAN/CNES), IBGE e Open-Meteo
                    para vigilância de dengue, previsão de risco climático e
                    encaminhamento inteligente de pacientes.

                    **GitHub:** https://github.com/fiap-tech-challenge-java/vigisus
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Grupo 7 — Hackathon FIAP 2026")
                    .url("https://github.com/fiap-tech-challenge-java/vigisus")))
            .tags(List.of(
                new Tag().name("Busca por Linguagem Natural").description("Pergunte em português livre"),
                new Tag().name("Epidemiologia").description("Histórico e ranking epidemiológico"),
                new Tag().name("Risco e Recursos").description("Previsão de risco e hospitais"),
                new Tag().name("Triagem").description("Triagem orientativa de pacientes"),
                new Tag().name("Encaminhamento").description("Encaminhamento hospitalar"),
                new Tag().name("Operacional").description("Pressão assistencial e protocolos"),
                new Tag().name("Admin").description("Administração interna")
            ));
    }
}
