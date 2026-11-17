package Jaja.Shop.API.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import Jaja.Shop.API.security.JwtProperties;

/*
  on active la lecture des réglages JWT et fournit l'horloge de l'application.
   L'horloge est un bean pour pouvoir la remplacer par une horloge fixe dans les tests
   (indispensable pour tester l'expiration d'un token sans attendre).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}