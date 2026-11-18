package com.dolcecandela.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuration métier et technique (préfixe "app" dans application.yml).
 * Validée au démarrage : l'application refuse de démarrer si un secret manque.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Business business,
        @Valid @NotNull Storage storage,
        @Valid @NotNull Security security) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET doit contenir au moins 32 caractères") String secret,
            @Min(5) long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Admin(String email, String password, String fullName) {
    }

    public record Business(
            @NotNull BigDecimal deliveryFee,
            @Min(0) @Max(100) int depositPercent,
            @NotBlank String pickupLocation) {
    }

    public record Storage(@NotBlank String uploadDir) {
    }

    public record Security(@Min(1) int maxLoginAttempts, @Min(1) int lockoutMinutes) {
    }
}
