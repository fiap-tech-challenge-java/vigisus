package br.com.fiap.vigisus;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class VigisusApplicationUnitTest {

    @Test
    void main_delegaParaSpringApplicationRun() {
        String[] args = {"--spring.main.banner-mode=off"};

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            VigisusApplication.main(args);
            mocked.verify(() -> SpringApplication.run(VigisusApplication.class, args));
        }
    }

    @Test
    void customOpenAPI_configuraInfoETagsPrincipais() {
        OpenAPI openAPI = new VigisusApplication().customOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).contains("V");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getDescription()).contains("DATASUS").contains("Hackathon FIAP");
        assertThat(openAPI.getTags()).hasSize(6);
        assertThat(openAPI.getTags().stream().map(tag -> tag.getName()).toList())
                .anySatisfy(name -> assertThat(name).contains("Perfil Epidemiol"))
                .anySatisfy(name -> assertThat(name).contains("Previs"))
                .anySatisfy(name -> assertThat(name).contains("Busca por Linguagem Natural"))
                .anySatisfy(name -> assertThat(name).contains("Administra"));
    }
}
