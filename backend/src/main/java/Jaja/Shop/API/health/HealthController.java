package Jaja.Shop.API.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Route de santé : permet de vérifier que l'API est démarrée et répond.
 *
 * @RestController = @Controller + @ResponseBody : la valeur retournée par
 * chaque méthode est écrite directement dans la réponse HTTP (ici en JSON),
 * au lieu d'être interprétée comme le nom d'une page HTML.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final String SERVICE_NAME = "jaja-shop-api";

    @GetMapping
    public Map<String, Object> health() {
        // Spring convertit cette Map en JSON grâce à Jackson.
        return Map.of(
                "status", "UP",
                "service", SERVICE_NAME,
                "timestamp", Instant.now().toString()
        );
    }
}