package Jaja.Shop.API.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
   Profil de l'utilisateur connecté (client ou administrateur).
   L'identité vient du token (@AuthenticationPrincipal), jamais de l'URL.
 */
@Tag(name = "Profil", description = "Consultation et modification du compte de l'utilisateur connecté")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Consulter mon profil",
            description = "Retourne les informations du compte de l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public UserResponse getProfile(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return profileService.getProfile(principal.id());
    }

    @Operation(summary = "Modifier mon profil",
            description = "Modifie le prénom, le nom et le téléphone. L'e-mail et le rôle ne sont pas modifiables.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping
    public UserResponse updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(principal.id(), request);
    }

    @Operation(summary = "Changer mon mot de passe",
            description = "Exige le mot de passe actuel. Le nouveau doit contenir 8 à 64 caractères, "
                    + "dont au moins une lettre et un chiffre, et différer de l'actuel.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mot de passe modifié"),
            @ApiResponse(responseCode = "400",
                    description = "Données invalides, mot de passe actuel incorrect (code INVALID_CURRENT_PASSWORD) "
                            + "ou nouveau mot de passe identique à l'actuel",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou compte introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(principal.id(), request);
        return ResponseEntity.noContent().build();
    }
}