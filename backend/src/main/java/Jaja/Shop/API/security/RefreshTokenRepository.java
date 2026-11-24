package Jaja.Shop.API.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /* Retrouve un token par son hash, avec son utilisateur déjà chargé (JOIN FETCH). */
    @Query("SELECT t FROM RefreshToken t JOIN FETCH t.user WHERE t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    /*
       Révoque un token SEULEMENT s'il est encore actif, en une seule opération atomique.
       Retourne 1 si ce token vient d'être "consommé", 0 s'il l'était déjà.
       Si deux requêtes simultanées présentent le même token, une seule obtient 1.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.id = :id AND t.revokedAt IS NULL")
    int revokeIfActive(@Param("id") Long id, @Param("now") Instant now);

    /* Révoque tous les tokens actifs d'une session (famille). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /* Révoque tous les tokens actifs d'un utilisateur (toutes ses sessions). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    /* Nettoyage : supprime les tokens expirés d'un utilisateur. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId AND t.expiresAt < :now")
    int deleteExpiredForUser(@Param("userId") Long userId, @Param("now") Instant now);
}