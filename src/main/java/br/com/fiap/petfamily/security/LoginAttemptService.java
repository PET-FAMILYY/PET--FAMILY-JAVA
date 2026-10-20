package br.com.fiap.petfamily.security;

import br.com.fiap.petfamily.exception.TentativasExcedidasException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Bloqueia o e-mail por 15 min após 5 falhas seguidas (em memória; ver nota sobre múltiplas instâncias). */
@Service
public class LoginAttemptService {

    private static final int MAX_FALHAS = 5;

    private final Cache<String, Integer> falhas = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(10_000)
            .build();

    public void exigirNaoBloqueado(String email) {
        Integer n = falhas.getIfPresent(email);
        if (n != null && n >= MAX_FALHAS) {
            throw new TentativasExcedidasException();
        }
    }

    public void registrarFalha(String email) {
        falhas.asMap().merge(email, 1, Integer::sum);
    }

    public void limpar(String email) {
        falhas.invalidate(email);
    }
}