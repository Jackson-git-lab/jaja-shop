package Jaja.Shop.API.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/*
   Test unitaire : MockEnvironment simule les variables d'environnement.
   On fait comme si elles existaient, sans démarrer Spring ni toucher à la
   vraie configuration — le test est donc rapide et isolé.
   
 */

class ProductionEnvironmentCheckTest {

    @Test
    void refuseDeDemarrerQuandDesVariablesManquent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_HOST", "localhost");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ProductionEnvironmentCheck(environment));

        assertTrue(exception.getMessage().contains("DB_PORT"));
        assertTrue(exception.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void accepteLeDemarrageQuandToutEstDefini() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_HOST", "localhost")
                .withProperty("DB_PORT", "5432")
                .withProperty("DB_NAME", "jajashop")
                .withProperty("DB_USER", "jajashop")
                .withProperty("DB_PASSWORD", "secret");

        assertDoesNotThrow(() -> new ProductionEnvironmentCheck(environment));
    }
}