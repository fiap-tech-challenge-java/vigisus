package br.com.fiap.vigisus;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
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
                .title("VígiSUS API")
                .version("1.0.0")
                .description("""
                    API pública de vigilância epidemiológica do SUS.

                    Transforma dados públicos do DATASUS, IBGE e Open-Meteo
                    em informação útil para gestores, profissionais de saúde
                    e cidadãos — sem login, sem cadastro.

                    Fontes de dados:
                    - SINAN (DATASUS): casos de dengue por município/semana
                    - CNES (DATASUS): hospitais, leitos e serviços
                    - IBGE: municípios e população
                    - Open-Meteo: clima atual e previsão 16 dias

                    Desenvolvido para o Hackathon FIAP Pós Tech — Fase 5
                    Tema: Inovação para otimização de atendimento no SUS
                    """)
                .contact(new Contact()
                    .name("VígiSUS")
                    .url("https://github.com/seu-usuario/vigisus"))
            )
            .addTagsItem(new Tag()
                .name("Perfil Epidemiológico")
                .description("Histórico de casos por município e período"))
            .addTagsItem(new Tag()
                .name("Previsão de Risco")
                .description("Score de risco baseado em clima + histórico"))
            .addTagsItem(new Tag()
                .name("Encaminhamento de Pacientes")
                .description("Hospital mais próximo com estrutura adequada"))
            .addTagsItem(new Tag()
                .name("Ranking Municipal")
                .description("Municípios ordenados por incidência"))
            .addTagsItem(new Tag()
                .name("Busca por Linguagem Natural")
                .description("Interpreta perguntas em português e retorna dados"))
            .addTagsItem(new Tag()
                .name("Administração")
                .description("Operações administrativas — limpeza de cache"));
    }
}
