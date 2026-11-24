package Jaja.Shop.API.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.user.User;

/*
   Gestion des refresh tokens : émission, rotation, révocation.
   Principes :
   - le token est une valeur aléatoire opaque de 256 bits ; seul son hash SHA-256 est stocké ;
   - à chaque rafraîchissement, l'ancien token est révoqué et un nouveau est émis (rotation) ;
   - si un token déjà révoqué est présenté de nouveau, toute la session (famille) est révoquée.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    /* Résultat d'une rotation : l'utilisateur concerné et le NOUVEAU refresh token (en clair, une seule fois). */
    public record Rotation(User user, String refreshToken) {
    }

    public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties,
                               Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /* Démarre une nouvelle session (connexion) : nouvelle famille, premier refresh token. */
    @Transactional
    public String issueForNewSession(User user) {
        Instant now = clock.instant();
        repository.deleteExpiredForUser(user.getId(), now);
        return create(user, UUID.randomUUID(), now);
    }

    /*
       Échange un refresh token contre un nouveau (rotation).
       noRollbackFor : quand on détecte une réutilisation, on révoque la famille PUIS on lance une
       exception 401. Sans cette option, la transaction serait annulée et la révocation avec elle.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken token = repository.findByTokenHash(hash(rawToken)).orElseThrow(this::invalid);

        // Token déjà utilisé ou révoqué : client bogué ou vol. On coupe toute la session.
        if (token.isRevoked()) {
            repository.revokeFamily(token.getFamilyId(), now);
            throw invalid();
        }
        if (token.isExpiredAt(now)) {
            throw invalid();
        }
        User user = token.getUser();
        if (!user.isEnabled()) {
            repository.revokeFamily(token.getFamilyId(), now);
            throw invalid();
        }

        // Consommation atomique : si deux requêtes arrivent en même temps avec le même token,
        // une seule gagne ; l'autre est traitée comme une réutilisation.
        int consumed = repository.revokeIfActive(token.getId(), now);
        if (consumed == 0) {
            repository.revokeFamily(token.getFamilyId(), now);
            throw invalid();
        }

        return new Rotation(user, create(user, token.getFamilyId(), now));
    }

    /* Déconnexion : révoque toute la session du token. Silencieux si le token est inconnu (idempotent). */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    /* Déconnecte l'utilisateur de tous ses appareils. */
    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }

    private String create(User user, UUID familyId, Instant now) {
        String rawToken = generateRawToken();
        Instant expiresAt = now.plus(Duration.ofDays(properties.expirationDays()));
        repository.save(new RefreshToken(user, hash(rawToken), familyId, now, expiresAt));
        return rawToken;
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /* Hash SHA-256 en hexadécimal (64 caractères). */
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private UnauthorizedException invalid() {
        return new UnauthorizedException("Refresh token invalide ou expiré.");
    }
}