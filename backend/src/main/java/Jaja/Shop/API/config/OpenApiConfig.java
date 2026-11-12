package Jaja.Shop.API.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
   Description générale de l'API et déclaration du schéma d'authentification JWT.
   Le schéma est seulement DÉCLARÉ ici. Chaque contrôleur protégé l'activera avec
   @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH), ce qui affiche un cadenas
   sur ses routes et permet à Swagger UI d'envoyer le token.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI jajaShopOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jaja Shop API")
                        .version("0.1.0")
                        .description("API de la boutique en ligne Jaja Shop : catalogue, panier, "
                                + "commandes, paiement Wave et administration."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenu à la connexion. "
                                        + "Collez uniquement le token, sans le mot « Bearer ».")));
    }
}