package Jaja.Shop.API.user;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import Jaja.Shop.API.auth.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/*
   Crée l'administrateur initial au démarrage de l'application, s'il n'existe aucun administrateur.
   Un ApplicationRunner s'exécute une fois, quand Spring a fini de démarrer
   (donc après Flyway : la table users existe).
   Règles de sécurité :
   - idempotent : si un administrateur existe, il ne se passe rien ;
   - un compte existant n'est JAMAIS promu administrateur (voir plus bas) ;
   - le mot de passe n'est jamais écrit dans les logs.
 */
@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    // BCrypt ne lit que les 72 premiers octets d'un mot de passe.
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final AdminProperties properties;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            Validator validator, AdminProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.debug("Un administrateur existe déjà : aucune création nécessaire.");
            return;
        }

        if (!properties.isConfigured()) {
            log.warn("Aucun administrateur n'existe et ADMIN_EMAIL / ADMIN_PASSWORD ne sont pas définis : "
                    + "personne ne pourra utiliser les routes /api/admin.");
            return;
        }

        checkRules();

        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            // On ne promeut JAMAIS un compte existant : l'inscription est publique, quelqu'un aurait pu
            // créer ce compte lui-même avant le premier démarrage pour devenir administrateur.
            throw new IllegalStateException("Impossible de créer l'administrateur initial : l'e-mail "
                    + email + " est déjà utilisé par un compte non administrateur. "
                    + "Choisissez un autre ADMIN_EMAIL ou supprimez ce compte.");
        }

        User admin = new User(
                email,
                passwordEncoder.encode(properties.password()),
                properties.firstName().trim(),
                properties.lastName().trim(),
                properties.phone().trim(),
                Role.ADMIN);

        try {
            userRepository.saveAndFlush(admin);
            log.info("Administrateur initial créé : {}", email);
        } catch (DataIntegrityViolationException e) {
            // Deux instances démarrées en même temps : l'autre a gagné, la contrainte UNIQUE a protégé la base.
            log.info("L'administrateur initial vient d'être créé par une autre instance.");
        }
    }

    /*
       Applique les mêmes règles que l'inscription (e-mail valide, mot de passe de 8 à 64 caractères
       avec une lettre et un chiffre ) en réutilisant la validation de RegisterRequest.
       Les messages ne contiennent jamais la valeur du mot de passe.
     */
    private void checkRules() {
        RegisterRequest candidate = new RegisterRequest(
                properties.firstName(), properties.lastName(), properties.email(),
                properties.phone(), properties.password());

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(candidate);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " : " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(" ; "));
            throw new IllegalStateException(
                    "Configuration de l'administrateur initial invalide : " + details);
        }

        if (properties.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new IllegalStateException(
                    "Configuration de l'administrateur initial invalide : ADMIN_PASSWORD est trop long.");
        }
    }
}