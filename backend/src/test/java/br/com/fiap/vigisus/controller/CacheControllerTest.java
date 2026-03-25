package br.com.fiap.vigisus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheControllerTest {

    @Test
    void limparCaches_limpaTodosOsCachesDisponiveis() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cacheA = mock(Cache.class);
        Cache cacheB = mock(Cache.class);
        CacheController controller = new CacheController(cacheManager);

        when(cacheManager.getCacheNames()).thenReturn(List.of("a", "b", "c"));
        when(cacheManager.getCache("a")).thenReturn(cacheA);
        when(cacheManager.getCache("b")).thenReturn(cacheB);
        when(cacheManager.getCache("c")).thenReturn(null);

        var response = controller.limparCaches();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("mensagem", "Caches limpos com sucesso"));
        verify(cacheA).clear();
        verify(cacheB).clear();
    }
}
