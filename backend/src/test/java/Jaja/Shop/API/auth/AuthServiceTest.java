package Jaja.Shop.API.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import Jaja.Shop.API.common.error.BadRequestException;
import Jaja.Shop.API.common.error.ConflictException;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;
import Jaja.Shop.API.user.UserRepository;
import Jaja.Shop.API.user.UserResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest() {
        return new RegisterRequest("Awa", "Kone", "  Awa.Kone@Example.com", "+225 07 12 34 56 78",
                "MotDePasse123");
    }

    @Test
    void creeUnCompteClientAvecMotDePasseHacheEtEmailNormalise() {
        when(userRepository.existsByEmail("awa.kone@example.com")).thenReturn(false);
        when(passwordEncoder.encode("MotDePasse123")).thenReturn("hash-bcrypt");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(validRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertEquals("awa.kone@example.com", saved.getEmail());
        assertEquals("hash-bcrypt", saved.getPasswordHash());
        assertNotEquals("MotDePasse123", saved.getPasswordHash());
        assertEquals(Role.CLIENT, saved.getRole());
        assertEquals(Role.CLIENT, response.role());
        assertEquals("awa.kone@example.com", response.email());
    }

    @Test
    void refuseUnEmailDejaUtilise() {
        when(userRepository.existsByEmail("awa.kone@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(validRequest()));

        verify(userRepository, never()).saveAndFlush(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void transformeLaContrainteUniqueDeLaBaseEnConflit() {
        when(userRepository.existsByEmail("awa.kone@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash-bcrypt");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(ConflictException.class, () -> authService.register(validRequest()));
    }

    @Test
    void refuseUnMotDePasseDepassantLaLimiteBcrypt() {
        // 74 caractères ASCII = 74 octets, au-dessus de la limite de 72
        RegisterRequest tooLong = new RegisterRequest("Awa", "Kone", "awa@example.com",
                "+225 07 12 34 56 78", "a1".repeat(37));

        assertThrows(BadRequestException.class, () -> authService.register(tooLong));

        verifyNoInteractions(userRepository, passwordEncoder);
    }
}