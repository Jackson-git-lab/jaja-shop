package Jaja.Shop.API.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/* Changement de mot de passe : l'ancien est exigé, le nouveau suit les règles de l'inscription. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangePasswordRequest(

        @Schema(description = "Mot de passe actuel", example = "MotDePasse123",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Le mot de passe actuel est obligatoire.")
        String currentPassword,

        @Schema(description = "Nouveau mot de passe : 8 à 64 caractères, au moins une lettre et un chiffre",
                example = "NouveauPass456", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = 8, max = 64, message = "Le nouveau mot de passe doit contenir entre 8 et 64 caractères.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Le nouveau mot de passe doit contenir au moins une lettre et un chiffre.")
        String newPassword
) {

    /* Les mots de passe n'apparaissent jamais dans les logs. */
    @Override
    public String toString() {
        return "ChangePasswordRequest[hidden]";
    }
}