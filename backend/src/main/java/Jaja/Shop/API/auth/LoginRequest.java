
package Jaja.Shop.API.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/* Données reçues à la connexion. */
public record LoginRequest(

        @Schema(description = "Adresse e-mail du compte", example = "awa.kone@example.com")
        @NotBlank(message = "L'e-mail est obligatoire.")
        String email,

        @Schema(description = "Mot de passe", example = "MotDePasse123",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password
) {

    /* Le mot de passe n'apparaît jamais dans les logs. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}