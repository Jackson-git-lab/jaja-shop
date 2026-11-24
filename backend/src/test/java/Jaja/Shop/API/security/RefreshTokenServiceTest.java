package Jaja.Shop.API.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");
    private static final UUID FAMILY = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RAW_TOKEN = "jeton-en-clair-presente-par-le-client";

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, new RefreshTokenProperties(7),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User user(boolean enabled) {
        User user = new User("awa@example.com", "hash", "Awa", "Kone", "+225 07 12 34 56 78", Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "enabled", enabled);
        return user;
    }

    private RefreshToken storedToken(User user, Instant expiresAt, Instant revokedAt) {
        RefreshToken token = new RefreshToken(user, "hash-stocke", FAMILY, NOW.minus(Duration.ofDays(1)), expiresAt);
        ReflectionTestUtils.setField(token, "id", 10L);
        ReflectionTestUtils.setField(token, "revokedAt", revokedAt);
        return token;
    }

    private void givenStored(RefreshToken token) {
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW_TOKEN))).thenReturn(Optional.of(token));
    }

    @Test
    void uneNouvelleSessionStockeSeulementLeHashAvecUneExpirationDeSeptJours() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String raw = service.issueForNewSession(user(true));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertEquals(RefreshTokenService.hash(raw), saved.getTokenHash());
        assertNotEquals(raw, saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
        assertNotNull(saved.getFamilyId());
        assertEquals(NOW, saved.getCreatedAt());
        assertEquals(NOW.plus(Duration.ofDays(7)), saved.getExpiresAt());
    }

    @Test
    void uneNouvelleSessionNettoieLesTokensExpiresDeLUtilisateur() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.issueForNewSession(user(true));

        verify(repository).deleteExpiredForUser(42L, NOW);
    }

    @Test
    void laRotationRevoqueLAncienTokenEtEmetUnNouveauDansLaMemeFamille() {
        User user = user(true);
        givenStored(storedToken(user, NOW.plus(Duration.ofDays(3)), null));
        when(repository.revokeIfActive(10L, NOW)).thenReturn(1);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.Rotation rotation = service.rotate(RAW_TOKEN);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken newToken = captor.getValue();
        assertEquals(FAMILY, newToken.getFamilyId());
        assertEquals(NOW.plus(Duration.ofDays(7)), newToken.getExpiresAt());
        assertEquals(RefreshTokenService.hash(rotation.refreshToken()), newToken.getTokenHash());
        assertNotEquals(RAW_TOKEN, rotation.refreshToken());
        assertSame(user, rotation.user());
        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void unTokenInconnuEstRefuse() {
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW_TOKEN))).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> service.rotate(RAW_TOKEN));

        verify(repository, never()).save(any());
    }

    @Test
    void laReutilisationDUnTokenDejaRevoqueCouperToutLaSession() {
        givenStored(storedToken(user(true), NOW.plus(Duration.ofDays(3)), NOW.minus(Duration.ofMinutes(5))));

        assertThrows(UnauthorizedException.class, () -> service.rotate(RAW_TOKEN));

        verify(repository).revokeFamily(FAMILY, NOW);
        verify(repository, never()).revokeIfActive(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void unTokenExpireEstRefuseSansEmettreDeNouveauToken() {
        givenStored(storedToken(user(true), NOW.minus(Duration.ofMinutes(1)), null));

        assertThrows(UnauthorizedException.class, () -> service.rotate(RAW_TOKEN));

        verify(repository, never()).revokeIfActive(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void siUneAutreRequeteAConsommeLeTokenEnMemeTempsLaSessionEstRevoquee() {
        givenStored(storedToken(user(true), NOW.plus(Duration.ofDays(3)), null));
        when(repository.revokeIfActive(10L, NOW)).thenReturn(0);

        assertThrows(UnauthorizedException.class, () -> service.rotate(RAW_TOKEN));

        verify(repository).revokeFamily(FAMILY, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void unCompteDesactiveNePeutPasRafraichirEtSaSessionEstRevoquee() {
        givenStored(storedToken(user(false), NOW.plus(Duration.ofDays(3)), null));

        assertThrows(UnauthorizedException.class, () -> service.rotate(RAW_TOKEN));

        verify(repository).revokeFamily(FAMILY, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void laDeconnexionRevoqueToutLaSessionDuToken() {
        givenStored(storedToken(user(true), NOW.plus(Duration.ofDays(3)), null));

        service.revoke(RAW_TOKEN);

        verify(repository).revokeFamily(FAMILY, NOW);
    }

    @Test
    void laDeconnexionAvecUnTokenInconnuNeFaitRienEtNeLanceAucuneErreur() {
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW_TOKEN))).thenReturn(Optional.empty());

        service.revoke(RAW_TOKEN);

        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void deconnecterTousLesAppareilsRevoqueTousLesTokensDeLUtilisateur() {
        service.revokeAllForUser(42L);

        verify(repository).revokeAllForUser(42L, NOW);
    }
}