package Jaja.Shop.API.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;
import io.jsonwebtoken.security.WeakKeyException;

/*
  Test unitaire du service JWT, sans Spring. Une horloge FIXE permet de simuler le temps qui passe.
 */
class JwtServiceTest {

    private static final String SECRET = "cle-de-test-uniquement-0123456789-abcdefghij";
    private static final String OTHER_SECRET = "une-autre-cle-de-test-9876543210-zyxwvutsrq";
    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

    private JwtService serviceAt(Instant instant, String secret) {
        return new JwtService(new JwtProperties(secret, 60), Clock.fixed(instant, ZoneOffset.UTC));
    }

    private User user() {
        User user = new User("awa.kone@example.com", "hash", "Awa", "Kone",
                "+225 07 12 34 56 78", Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    @Test
    void leTokenContientLesInformationsDeLUtilisateurEtUneExpiration() {
        JwtService service = serviceAt(NOW, SECRET);

        JwtClaims claims = service.parse(service.generateToken(user()));

        assertEquals(42L, claims.userId());
        assertEquals("awa.kone@example.com", claims.email());
        assertEquals(Role.CLIENT, claims.role());
        assertEquals(NOW, claims.issuedAt());
        assertEquals(NOW.plus(Duration.ofMinutes(60)), claims.expiresAt());
        assertEquals(3600L, service.getExpiresInSeconds());
    }

    @Test
    void leTokenEstValideAvantExpirationEtRefuseApres() {
        String token = serviceAt(NOW, SECRET).generateToken(user());

        assertDoesNotThrow(() -> serviceAt(NOW.plus(Duration.ofMinutes(59)), SECRET).parse(token));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> serviceAt(NOW.plus(Duration.ofMinutes(61)), SECRET).parse(token));
        assertEquals("Token invalide ou expiré.", exception.getMessage());
    }

    @Test
    void unTokenSigneAvecUneAutreCleEstRefuse() {
        String token = serviceAt(NOW, OTHER_SECRET).generateToken(user());

        assertThrows(UnauthorizedException.class, () -> serviceAt(NOW, SECRET).parse(token));
    }

    @Test
    void unTokenModifieEstRefuse() {
        String token = serviceAt(NOW, SECRET).generateToken(user());

        // On altère un caractère du contenu : la signature ne correspond plus.
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[10] = payload[10] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThrows(UnauthorizedException.class, () -> serviceAt(NOW, SECRET).parse(tampered));
    }

    @Test
    void unTexteQuiNEstPasUnJwtEstRefuse() {
        assertThrows(UnauthorizedException.class, () -> serviceAt(NOW, SECRET).parse("pas-un-jwt"));
    }

    @Test
    void unSecretTropCourtEmpecheLeDemarrage() {
        assertThrows(WeakKeyException.class, () -> serviceAt(NOW, "trop-court"));
    }
}