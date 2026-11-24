package Jaja.Shop.API.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
   Données d'une adresse (création et modification).
   Pas de champ userId : le propriétaire est toujours l'utilisateur du token.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AddressRequest(

        @Schema(description = "Nom donné à l'adresse", example = "Maison")
        @NotBlank(message = "Le libellé est obligatoire.")
        @Size(max = 50, message = "Le libellé ne doit pas dépasser 50 caractères.")
        String label,

        @Schema(description = "Ville", example = "Abidjan")
        @NotBlank(message = "La ville est obligatoire.")
        @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
        String city,

        @Schema(description = "Quartier ou commune", example = "Cocody")
        @NotBlank(message = "Le quartier est obligatoire.")
        @Size(max = 100, message = "Le quartier ne doit pas dépasser 100 caractères.")
        String district,

        @Schema(description = "Précisions pour le livreur", example = "Rue 12, près de la pharmacie")
        @NotBlank(message = "Les détails de l'adresse sont obligatoires.")
        @Size(max = 255, message = "Les détails ne doivent pas dépasser 255 caractères.")
        String details,

        @Schema(description = "Téléphone de contact pour la livraison", example = "+225 07 12 34 56 78")
        @NotBlank(message = "Le téléphone est obligatoire.")
        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$",
                message = "Le téléphone doit contenir 8 à 20 chiffres (le + et les espaces sont autorisés).")
        String phone,

        @Schema(description = "true : cette adresse devient l'adresse par défaut. "
                + "false ou absent : le statut par défaut ne change pas (la première adresse est toujours par défaut).")
        Boolean defaultAddress
) {
}