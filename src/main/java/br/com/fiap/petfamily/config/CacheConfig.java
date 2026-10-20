package br.com.fiap.petfamily.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "tutores", "pets", "consultas", "lembretes", "dashboard"
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }

    CaffeineCacheManager manager = new CaffeineCacheManager(
            CacheNames.TUTORES, CacheNames.PETS, CacheNames.CONSULTAS,
            CacheNames.LEMBRETES, CacheNames.DASHBOARD);
}
