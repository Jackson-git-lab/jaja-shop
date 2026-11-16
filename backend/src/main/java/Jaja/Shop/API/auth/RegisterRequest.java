package Jaja.Shop.API.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
   Données reçues à l'inscription.
   Il n'y a volontairement PAS de champ "role" : le rôle est imposé par le serveur (CLIENT).
   ignoreUnknown = true : un champ inconnu envoyé par un client (ex. "role": "ADMIN")
   est simplement ignoré, il ne peut rien modifier.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(

        @Schema(description = "Prénom", example = "Awa")
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
        String firstName,

        @Schema(description = "Nom de famille", example = "Kone")
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
        String lastName,

        @Schema(description = "Adresse e-mail (sert d'identifiant de connexion)", example = "awa.kone@example.com")
        @NotBlank(message = "L'e-mail est obligatoire.")
        @Email(message = "L'e-mail n'est pas valide.")
        @Size(max = 255, message = "L'e-mail ne doit pas dépasser 255 caractères.")
        String email,

        @Schema(description = "Numéro de téléphone", example = "+225 07 12 34 56 78")
        @NotBlank(message = "Le téléphone est obligatoire.")
        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$",
                message = "Le téléphone doit contenir 8 à 20 chiffres (le + et les espaces sont autorisés).")
        String phone,

        @Schema(description = "Mot de passe : 8 à 64 caractères, au moins une lettre et un chiffre",
                example = "MotDePasse123", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 64, message = "Le mot de passe doit contenir entre 8 et 64 caractères.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir au moins une lettre et un chiffre.")
        String password
) {

    /*
       Le toString d'un record affiche tous ses champs : un simple log aurait écrit le mot de passe.
       On le redéfinit pour ne jamais l'exposer.
     */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + "]";
    }
}