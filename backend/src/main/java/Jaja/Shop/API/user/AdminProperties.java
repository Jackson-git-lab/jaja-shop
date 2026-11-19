package Jaja.Shop.API.user;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/*
   Données de l'administrateur initial, lues dans la configuration (préfixe "app.admin").
   En pratique, elles viennent des variables d'environnement ADMIN_EMAIL, ADMIN_PASSWORD, etc.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password, String firstName, String lastName, String phone) {

    /* La création n'a lieu que si l'e-mail ET le mot de passe sont renseignés. */
    public boolean isConfigured() {
        return StringUtils.hasText(email) && StringUtils.hasText(password);
    }

    /* Le mot de passe n'apparaît jamais dans les logs, même si l'objet est affiché. */
    @Override
    public String toString() {
        return "AdminProperties[email=" + email + "]";
    }
}