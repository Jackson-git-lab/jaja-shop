package Jaja.Shop.API.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.security.JwtService;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;
import Jaja.Shop.API.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    private static final String INVALID_CREDENTIALS = "Identifiants invalides.";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user() {
        return new User("awa.kone@example.com", "hash-bcrypt", "Awa", "Kone",
                "+225 07 12 34 56 78", Role.CLIENT);
    }

    @Test
    void connexionReussieRetourneUnTokenEtLUtilisateur() {
        User user = user();
        when(userRepository.findByEmail("awa.kone@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("MotDePasse123", "hash-bcrypt")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpiresInSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(new LoginRequest("awa.kone@example.com", "MotDePasse123"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("awa.kone@example.com", response.user().email());
        assertEquals(Role.CLIENT, response.user().role());
    }

    @Test
    void lEmailEstNormaliseAvantLaRecherche() {
        User user = user();
        when(userRepository.findByEmail("awa.kone@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("MotDePasse123", "hash-bcrypt")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("  Awa.Kone@EXAMPLE.com ", "MotDePasse123"));

        assertEquals("jwt-token", response.accessToken());
    }

    @Test
    void emailInconnuRepond401ApresUneVerificationBcryptFactice() {
        when(userRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("inconnu@example.com", "MotDePasse123")));

        assertEquals(INVALID_CREDENTIALS, exception.getMessage());
        // Le travail BCrypt est fait même sans compte : le temps de réponse ne révèle rien.
        verify(passwordEncoder).matches(eq("MotDePasse123"), anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void mauvaisMotDePasseRepond401AvecLeMemeMessage() {
        when(userRepository.findByEmail("awa.kone@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("mauvais1234", "hash-bcrypt")).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("awa.kone@example.com", "mauvais1234")));

        assertEquals(INVALID_CREDENTIALS, exception.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void compteDesactiveRepond401MemeAvecLeBonMotDePasse() {
        User disabled = user();
        ReflectionTestUtils.setField(disabled, "enabled", false);
        when(userRepository.findByEmail("awa.kone@example.com")).thenReturn(Optional.of(disabled));
        when(passwordEncoder.matches("MotDePasse123", "hash-bcrypt")).thenReturn(true);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("awa.kone@example.com", "MotDePasse123")));

        assertEquals(INVALID_CREDENTIALS, exception.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void motDePasseTropLongRepond401SansInterrogerLaBase() {
        String tooLong = "a1".repeat(37);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("awa.kone@example.com", tooLong)));

        verifyNoInteractions(userRepository, passwordEncoder, jwtService);
    }
}