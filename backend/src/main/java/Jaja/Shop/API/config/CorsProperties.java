package Jaja.Shop.API.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/*
   Origines autorisées à appeler l'API depuis un navigateur (CORS).
   Plusieurs origines se séparent par des virgules dans la variable CORS_ALLOWED_ORIGINS.
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}