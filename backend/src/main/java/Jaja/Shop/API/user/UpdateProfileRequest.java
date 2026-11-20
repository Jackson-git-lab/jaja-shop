package Jaja.Shop.API.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/* Données modifiables du profil.  Pas de champ email ni role : un champ inconnu envoyé par un client est simplement ignoré. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateProfileRequest(

        @Schema(description = "Prénom", example = "Awa")
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
        String firstName,

        @Schema(description = "Nom de famille", example = "Kone")
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
        String lastName,

        @Schema(description = "Numéro de téléphone", example = "+225 07 00 00 00 00")
        @NotBlank(message = "Le téléphone est obligatoire.")
        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$",
                message = "Le téléphone doit contenir 8 à 20 chiffres (le + et les espaces sont autorisés).")
        String phone
) {
}