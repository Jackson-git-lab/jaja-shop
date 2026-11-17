package Jaja.Shop.API.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Jaja.Shop.API.common.error.BadRequestException;
import Jaja.Shop.API.common.error.ConflictException;
import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.security.JwtService;
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
    private static final String TOKEN_TYPE = "Bearer";


    // Hash BCrypt valide d'un mot de passe factice. Il sert à faire le même travail de calcul
    // quand l'e-mail n'existe pas pour que le temps de réponse ne révèle pas si un compte existe.

    private static final String DUMMY_HASH =
            "$2b$10$4iamzI837aYUzilh4Z7FJOPvvJzlU3mxmJnRKzB5dVzBTRS1LkEYO";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String password = request.password();
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            // Aucun mot de passe enregistré ne peut dépasser cette taille.
            throw invalidCredentials();
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> found = userRepository.findByEmail(email);

        // La vérification BCrypt est TOUJOURS exécutée, même si l'e-mail n'existe pas.
        String hashToCheck = found.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);

        // Même erreur dans tous les cas d'échec : on ne révèle pas ce qui ne va pas.
        if (found.isEmpty() || !passwordMatches || !found.get().isEnabled()) {
            throw invalidCredentials();
        }

        User user = found.get();
        return new LoginResponse(
                jwtService.generateToken(user),
                TOKEN_TYPE,
                jwtService.getExpiresInSeconds(),
                UserResponse.from(user));
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Identifiants invalides.");
    }
}