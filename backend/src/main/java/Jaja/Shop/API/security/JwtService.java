package Jaja.Shop.API.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/*
   Création et lecture des tokens JWT.
   Un JWT a trois parties séparées par des points : en-tête.contenu.signature.
   Le contenu (payload) est lisible par tout le monde : il est SIGNÉ, pas chiffré.
   On n'y met donc jamais d'information sensible (pas de mot de passe).
 */
@Service
public class JwtService {

    private static final String ISSUER = "jaja-shop-api";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration validity;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        // Lève une WeakKeyException si le secret fait moins de 256 bits.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.validity = Duration.ofMinutes(properties.expirationMinutes());
        this.clock = clock;
    }

    /* on crée un token signé (HS256) contenant l'identifiant, l'e-mail et le rôle de l'utilisateur. */
    public String generateToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /* Durée de vie du token, en secondes (renvoyée au client à la connexion). */
    public long getExpiresInSeconds() {
        return validity.toSeconds();
    }

    /*
      on vérifie la signature, l'émetteur et l'expiration, puis retourne le contenu.
      Toute anomalie (token modifié, expiré, mal formé, mauvaise clé) donne la même erreur 401.
     */
    public JwtClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new JwtClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Token invalide ou expiré.");
        }
    }
}