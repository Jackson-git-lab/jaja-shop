package Jaja.Shop.API.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
   Réglages des refresh tokens (préfixe "app.refresh-token").
   @param expirationDays durée de vie d'un refresh token, en jours
 */
@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(@Min(1) long expirationDays) {
}