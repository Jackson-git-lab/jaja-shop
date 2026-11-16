package Jaja.Shop.API.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

class RegisterRequestTest {

    @Test
    void toStringNeRevelePasLeMotDePasse() {
        RegisterRequest request = new RegisterRequest("Awa", "Kone", "awa@example.com",
                "+225 07 12 34 56 78", "MotDePasse123");

        assertFalse(request.toString().contains("MotDePasse123"));
    }
}