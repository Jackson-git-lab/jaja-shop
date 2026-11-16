package Jaja.Shop.API.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
   Réglages JWT lus depuis la configuration (préfixe "app.jwt").
   La validation fait échouer le démarrage si le secret est absent ou trop court (fail fast).
 
   @param secret  secret de signature, 32 caractères minimum (256 bits pour HS256)
   @param expirationMinutes durée de vie du token, en minutes
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "app.jwt.secret doit contenir au moins 32 caractères.")
        String secret,
        @Min(1) long expirationMinutes
) {

    /* on evite d'écrire le secret dans les logs si l'objet est affiché. */
    @Override
    public String toString() {
        return "JwtProperties[expirationMinutes=" + expirationMinutes + "]";
    }
}