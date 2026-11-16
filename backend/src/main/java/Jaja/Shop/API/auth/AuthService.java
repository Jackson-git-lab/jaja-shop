package Jaja.Shop.API.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Jaja.Shop.API.common.error.BadRequestException;
import Jaja.Shop.API.common.error.ConflictException;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;
import Jaja.Shop.API.user.UserRepository;
import Jaja.Shop.API.user.UserResponse;

/* Logique métier de l'authentification : pour l'instant, l'inscription. */
@Service
public class AuthService {

    // BCrypt ne lit que les 72 premiers octets d'un mot de passe.
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final String EMAIL_ALREADY_USED = "Un compte existe déjà avec cet e-mail.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BadRequestException("Le mot de passe est trop long.");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                Role.CLIENT);

        try {
            // saveAndFlush envoie l'INSERT tout de suite, pour détecter ici un doublon
            // créé entre notre vérification et l'enregistrement (deux inscriptions simultanées).
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }
    }
}