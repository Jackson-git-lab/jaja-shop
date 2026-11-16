package Jaja.Shop.API.security;

import java.time.Instant;

import Jaja.Shop.API.user.Role;

/* Informations extraites d'un token JWT valide. Réutilisé par le filtre de BE-09. */

public record JwtClaims(Long userId, String email, Role role, Instant issuedAt, Instant expiresAt) {
}