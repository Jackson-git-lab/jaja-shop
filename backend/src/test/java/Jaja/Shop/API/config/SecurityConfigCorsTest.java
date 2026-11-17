package Jaja.Shop.API.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigCorsTest {

    @Test
    void refuseLeJokerEtoileCommeOrigine() {
        CorsProperties wildcard = new CorsProperties(List.of("*"));

        assertThrows(IllegalStateException.class,
                () -> new SecurityConfig().corsConfigurationSource(wildcard));
    }

    @Test
    void limiteLesOriginesAuxValeursConfigurees() {
        List<String> origins = List.of("http://localhost:5173", "https://jajashop.example");

        UrlBasedCorsConfigurationSource source =
                new SecurityConfig().corsConfigurationSource(new CorsProperties(origins));

        CorsConfiguration configuration = source.getCorsConfigurations().get("/api/**");
        assertNotNull(configuration);
        assertEquals(origins, configuration.getAllowedOrigins());
        assertTrue(configuration.getAllowedMethods().contains("DELETE"));
        assertTrue(configuration.getAllowedHeaders().contains("Authorization"));
        assertFalse(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }
}