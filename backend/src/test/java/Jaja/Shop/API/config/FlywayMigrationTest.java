package Jaja.Shop.API.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
   On vérifie deux choses :
   1. Flyway a bien appliqué la migration V1 sur la base de test.
   2. Hibernate est en mode "validate" : il vérifie le schéma mais ne le
      crée pas et ne le modifie pas (c'est Flyway qui s'en occupe).
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Test
    void laMigrationV1EstAppliqueeAvecSucces() {
        // flyway_schema_history est la table où Flyway note chaque migration appliquée.
        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'",
                Boolean.class);

        assertEquals(Boolean.TRUE, success);
    }

    @Test
    void hibernateNeModifiePasLeSchema() {
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
    }
}