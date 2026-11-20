package Jaja.Shop.API.user;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Jaja.Shop.API.common.error.BadRequestException;
import Jaja.Shop.API.common.error.InvalidCurrentPasswordException;
import Jaja.Shop.API.common.error.UnauthorizedException;

/* Logique métier du profil de l'utilisateur connecté. */

@Service
public class ProfileService {

    // BCrypt ne lit que les 72 premiers octets d'un mot de passe.
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findActiveUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        user.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim());
        // Pas de save() : l'entité est « gérée » par JPA, la modification est envoyée
        // à la base à la fin de la transaction (dirty checking).
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        if (exceedsBcryptLimit(request.newPassword())) {
            throw new BadRequestException("Le nouveau mot de passe est trop long.");
        }
        // Un mot de passe de plus de 72 octets ne peut pas être celui d'un compte existant.
        if (exceedsBcryptLimit(request.currentPassword())
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'actuel.");
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    /*
       Le token prouve qui est l'utilisateur, mais le compte a pu être supprimé ou désactivé depuis :
       on relit la base à chaque appel.
     */
    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Compte introuvable ou désactivé."));
    }

    private boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}