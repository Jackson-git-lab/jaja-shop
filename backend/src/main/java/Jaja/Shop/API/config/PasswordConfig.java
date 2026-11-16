package Jaja.Shop.API.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
   Déclare l'encodeur de mots de passe utilisé par toute l'application.
   On injecte l'interface PasswordEncoder partout : on pourra changer d'algorithme
   sans modifier les services.
 */

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}