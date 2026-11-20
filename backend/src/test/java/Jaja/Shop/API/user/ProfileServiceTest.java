package Jaja.Shop.API.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import Jaja.Shop.API.common.error.BadRequestException;
import Jaja.Shop.API.common.error.InvalidCurrentPasswordException;
import Jaja.Shop.API.common.error.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private User givenActiveUser() {
        User user = new User("awa.kone@example.com", "hash-actuel", "Awa", "Kone",
                "+225 07 12 34 56 78", Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void getProfileRetourneLeProfilDeLUtilisateurActif() {
        givenActiveUser();

        UserResponse response = profileService.getProfile(USER_ID);

        assertEquals("awa.kone@example.com", response.email());
        assertEquals("Awa", response.firstName());
        assertEquals(Role.CLIENT, response.role());
    }

    @Test
    void getProfileRepond401SiLUtilisateurNExistePlus() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> profileService.getProfile(USER_ID));
    }

    @Test
    void getProfileRepond401SiLeCompteEstDesactive() {
        User user = givenActiveUser();
        ReflectionTestUtils.setField(user, "enabled", false);

        assertThrows(UnauthorizedException.class, () -> profileService.getProfile(USER_ID));
    }

    @Test
    void updateProfileModifieNomPrenomEtTelephoneSansToucherAuRestePourLesChampsProteges() {
        User user = givenActiveUser();

        UserResponse response = profileService.updateProfile(USER_ID,
                new UpdateProfileRequest("  Aminata ", " Traore", " +225 05 11 22 33 44 "));

        assertEquals("Aminata", user.getFirstName());
        assertEquals("Traore", user.getLastName());
        assertEquals("+225 05 11 22 33 44", user.getPhone());
        assertEquals("awa.kone@example.com", user.getEmail());
        assertEquals(Role.CLIENT, user.getRole());
        assertEquals("hash-actuel", user.getPasswordHash());
        assertEquals("Aminata", response.firstName());
    }

    @Test
    void changePasswordRemplaceLeHashQuandLAncienMotDePasseEstBon() {
        User user = givenActiveUser();
        when(passwordEncoder.matches("MotDePasse123", "hash-actuel")).thenReturn(true);
        when(passwordEncoder.encode("NouveauPass456")).thenReturn("nouveau-hash");

        profileService.changePassword(USER_ID, new ChangePasswordRequest("MotDePasse123", "NouveauPass456"));

        assertEquals("nouveau-hash", user.getPasswordHash());
    }

    @Test
    void changePasswordRefuseUnMauvaisMotDePasseActuel() {
        User user = givenActiveUser();
        when(passwordEncoder.matches("mauvais1234", "hash-actuel")).thenReturn(false);

        assertThrows(InvalidCurrentPasswordException.class,
                () -> profileService.changePassword(USER_ID,
                        new ChangePasswordRequest("mauvais1234", "NouveauPass456")));

        assertEquals("hash-actuel", user.getPasswordHash());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePasswordRefuseUnNouveauMotDePasseIdentiqueALAncien() {
        User user = givenActiveUser();
        when(passwordEncoder.matches("MotDePasse123", "hash-actuel")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> profileService.changePassword(USER_ID,
                        new ChangePasswordRequest("MotDePasse123", "MotDePasse123")));

        assertEquals("hash-actuel", user.getPasswordHash());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePasswordRefuseUnNouveauMotDePasseDepassantLaLimiteBcrypt() {
        givenActiveUser();

        assertThrows(BadRequestException.class,
                () -> profileService.changePassword(USER_ID,
                        new ChangePasswordRequest("MotDePasse123", "a1".repeat(37))));

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void changePasswordRefuseUnAncienMotDePasseDepassantLaLimiteBcryptSansInterrogerBcrypt() {
        givenActiveUser();

        assertThrows(InvalidCurrentPasswordException.class,
                () -> profileService.changePassword(USER_ID,
                        new ChangePasswordRequest("a1".repeat(37), "NouveauPass456")));

        verifyNoInteractions(passwordEncoder);
    }
}