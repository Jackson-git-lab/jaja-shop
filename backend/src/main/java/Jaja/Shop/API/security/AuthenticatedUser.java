package Jaja.Shop.API.security;

import Jaja.Shop.API.user.Role;

/*
   Utilisateur authentifié pour la requête en cours, reconstruit depuis le token JWT.
   Les contrôleurs le récupèrent avec @AuthenticationPrincipal AuthenticatedUser.
 */
public record AuthenticatedUser(Long id, String email, Role role) {
}