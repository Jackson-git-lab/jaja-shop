package Jaja.Shop.API.config;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/*
   En profil "prod" uniquement : on vérifie que toutes les variables
   d'environnement de la base de données sont présentes.
   Si une variable manque, l'application s'arrête immédiatement avec un
   message clair (fail fast) au lieu de démarrer avec une config incomplète.
 */


@Component
@Profile("prod")
public class ProductionEnvironmentCheck {

    private static final List<String> REQUIRED_VARIABLES =
            List.of("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD");

    public ProductionEnvironmentCheck(Environment environment) {
        List<String> missing = REQUIRED_VARIABLES.stream()
                .filter(name -> {
                    String value = environment.getProperty(name);
                    return value == null || value.isBlank();
                })
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Démarrage refusé en production : variables d'environnement manquantes : "
                            + String.join(", ", missing));
        }
    }
}