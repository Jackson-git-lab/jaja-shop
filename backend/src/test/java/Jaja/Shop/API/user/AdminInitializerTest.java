package Jaja.Shop.API.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    // Vrai validateur : on teste les vraies règles de validation, pas des faux.
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer initializer(String email, String password) {
        AdminProperties properties = new AdminProperties(
                email, password, "Admin", "Jaja Shop", "+225 00 00 00 00");
        return new AdminInitializer(userRepository, passwordEncoder, validator, properties);
    }

    @Test
    void creeLAdministrateurQuandAucunAdministrateurNExiste() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("AdminPass123")).thenReturn("hash-bcrypt");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer("Admin@JajaShop.com", "AdminPass123").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertEquals("admin@jajashop.com", saved.getEmail());
        assertEquals("hash-bcrypt", saved.getPasswordHash());
        assertEquals(Role.ADMIN, saved.getRole());
        assertTrue(saved.isEnabled());
    }

    @Test
    void neFaitRienQuandUnAdministrateurExisteDeja() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        initializer("admin@jajashop.com", "AdminPass123").run(null);

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void neFaitRienEtNePlantePasQuandLaConfigurationEstVide() {
        assertDoesNotThrow(() -> initializer("", "").run(null));
        assertDoesNotThrow(() -> initializer(null, null).run(null));

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void refuseDeDemarrerSiLEmailEstDejaPrisParUnCompteNonAdministrateur() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@jajashop.com")).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> initializer("admin@jajashop.com", "AdminPass123").run(null));

        assertTrue(exception.getMessage().contains("déjà utilisé"));
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void refuseUnMotDePasseFaibleSansLeRevelerDansLeMessage() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> initializer("admin@jajashop.com", "Zx9").run(null));

        assertTrue(exception.getMessage().contains("password"));
        assertFalse(exception.getMessage().contains("Zx9"), "le mot de passe ne doit jamais apparaître");
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void refuseUnEmailInvalide() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> initializer("pas-un-email", "AdminPass123").run(null));

        assertTrue(exception.getMessage().contains("email"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void tolereUneCreationSimultaneeParUneAutreInstance() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("AdminPass123")).thenReturn("hash-bcrypt");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDoesNotThrow(() -> initializer("admin@jajashop.com", "AdminPass123").run(null));
    }
}