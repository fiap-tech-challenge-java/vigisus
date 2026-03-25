package br.com.fiap.vigisus.config;

import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.IaServiceFallback;
import br.com.fiap.vigisus.service.IaServiceGeminiImpl;
import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IaConfigTest {

    @Test
    void iaService_retornaFallbackQuandoApiKeyAusente() {
        IaConfig config = new IaConfig();
        ReflectionTestUtils.setField(config, "apiKey", "");
        ReflectionTestUtils.setField(config, "model", "gemini-test");

        IaService service = config.iaService(providerVazio());

        assertThat(service).isInstanceOf(IaServiceFallback.class);
    }

    @Test
    void iaService_retornaGeminiQuandoHaApiKeyEClienteDisponivel() {
        IaConfig config = new IaConfig();
        ReflectionTestUtils.setField(config, "apiKey", "abc");
        ReflectionTestUtils.setField(config, "model", "gemini-test");
        Client client = mock(Client.class);

        IaService service = config.iaService(providerCom(client));

        assertThat(service).isInstanceOf(IaServiceGeminiImpl.class);
    }

    @Test
    void iaService_lancaQuandoApiKeyExisteMasClienteNaoFoiCriado() {
        IaConfig config = new IaConfig();
        ReflectionTestUtils.setField(config, "apiKey", "abc");
        ReflectionTestUtils.setField(config, "model", "gemini-test");

        assertThatThrownBy(() -> config.iaService(providerVazio()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cliente Gemini indisponivel");
    }

    private ObjectProvider<Client> providerCom(Client client) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("geminiClient", client);
        return factory.getBeanProvider(Client.class);
    }

    private ObjectProvider<Client> providerVazio() {
        return new DefaultListableBeanFactory().getBeanProvider(Client.class);
    }
}
