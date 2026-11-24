package Jaja.Shop.API.address;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Jaja.Shop.API.common.error.ApiError;
import Jaja.Shop.API.config.OpenApiConfig;
import Jaja.Shop.API.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/*
   Adresses de livraison de l'utilisateur connecté.
   Le propriétaire est toujours celui du token : aucun identifiant d'utilisateur dans l'URL.
 */
@Tag(name = "Adresses de livraison", description = "Carnet d'adresses de l'utilisateur connecté")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(summary = "Lister mes adresses",
            description = "Retourne mes adresses : l'adresse par défaut en premier, puis par ordre de création.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste de mes adresses"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<AddressResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return addressService.list(principal.id());
    }

    @Operation(summary = "Consulter une de mes adresses",
            description = "Retourne 404 si l'adresse n'existe pas ou n'appartient pas à l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adresse",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Adresse introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public AddressResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
                               @PathVariable Long id) {
        return addressService.get(principal.id(), id);
    }

    @Operation(summary = "Ajouter une adresse",
            description = "La première adresse est automatiquement l'adresse par défaut. "
                    + "Avec defaultAddress à true, la nouvelle adresse remplace l'ancienne adresse par défaut. "
                    + "Maximum 10 adresses par utilisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adresse créée",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Limite de 10 adresses atteinte",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse created = addressService.create(principal.id(), request);
        URI location = URI.create("/api/me/addresses/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Modifier une adresse",
            description = "Remplace les informations de l'adresse. defaultAddress à true en fait l'adresse par défaut ; "
                    + "false ou absent ne retire jamais le statut de l'adresse par défaut actuelle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adresse mise à jour",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Adresse introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public AddressResponse update(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.update(principal.id(), id, request);
    }

    @Operation(summary = "Supprimer une adresse",
            description = "Si l'adresse supprimée était l'adresse par défaut, la plus récente des adresses restantes "
                    + "devient l'adresse par défaut.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Adresse supprimée"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Adresse introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long id) {
        addressService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}