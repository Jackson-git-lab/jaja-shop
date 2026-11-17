package Jaja.Shop.API.auth;

import Jaja.Shop.API.user.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/* Réponse à une connexion réussie. */
public record LoginResponse(

        @Schema(description = "Token JWT à envoyer dans l'en-tête Authorization: Bearer <token>")
        String accessToken,

        @Schema(description = "Type de token", example = "Bearer")
        String tokenType,

        @Schema(description = "Durée de validité du token, en secondes", example = "3600")
        long expiresIn,

        @Schema(description = "Utilisateur connecté")
        UserResponse user
) {

    /* Le token est un secret temporaire : on ne l'écrit pas dans les logs. */
    @Override
    public String toString() {
        return "LoginResponse[user=" + user + "]";
    }
}