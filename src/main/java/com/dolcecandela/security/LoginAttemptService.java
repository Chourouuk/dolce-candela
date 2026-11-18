package com.dolcecandela.security;

import com.dolcecandela.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protection contre le brute-force : après N échecs, le compte est bloqué X minutes.
 * Stockage en mémoire (suffisant pour une seule instance ; utiliser Redis si on passe à plusieurs instances).
 */
@Service
public class LoginAttemptService {

    private record Attempts(int count, Instant firstFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockout;

    public LoginAttemptService(AppProperties properties) {
        this.maxAttempts = properties.security().maxLoginAttempts();
        this.lockout = Duration.ofMinutes(properties.security().lockoutMinutes());
    }

    public boolean isBlocked(String email) {
        Attempts current = attempts.get(normalize(email));
        if (current == null) {
            return false;
        }
        if (current.firstFailure().plus(lockout).isBefore(Instant.now())) {
            attempts.remove(normalize(email));
            return false;
        }
        return current.count() >= maxAttempts;
    }

    public void loginFailed(String email) {
        attempts.merge(normalize(email), new Attempts(1, Instant.now()),
                (old, ignored) -> old.firstFailure().plus(lockout).isBefore(Instant.now())
                        ? new Attempts(1, Instant.now())
                        : new Attempts(old.count() + 1, old.firstFailure()));
    }

    public void loginSucceeded(String email) {
        attempts.remove(normalize(email));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
