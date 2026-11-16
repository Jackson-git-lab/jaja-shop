package Jaja.Shop.API.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/*
   Test de bout en bout : vraie application, vraie base PostgreSQL de test (jajashop_test).
   La table users est vidée avant chaque test pour qu'ils restent indépendants.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthRegistrationIntegrationTest {

    private static final String VALID_BODY = """
            {"firstName":"Awa","lastName":"Kone","email":"Awa.Kone@Example.com",
             "phone":"+225 07 12 34 56 78","password":"MotDePasse123"}""";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void inscriptionCreeUnCompteClientSansFuiteDuMotDePasse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("awa.kone@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void leMotDePasseEstStockeHache() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, "awa.kone@example.com");

        assertNotEquals("MotDePasse123", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder().matches("MotDePasse123", hash));
    }

    @Test
    void emailDejaUtiliseRepond409MemeAvecUneCasseDifferente() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        String sameEmailOtherCase = VALID_BODY.replace("Awa.Kone@Example.com", "AWA.KONE@EXAMPLE.COM");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameEmailOtherCase))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void leRoleEnvoyeParLeClientEstIgnore() throws Exception {
        String bodyWithRole = VALID_BODY.replace("\"password\"", "\"role\":\"ADMIN\",\"password\"");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithRole))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENT"));

        String roleInBase = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE email = ?", String.class, "awa.kone@example.com");
        assertEquals("CLIENT", roleInBase);
    }

    @Test
    void donneesInvalidesRepond400EtNeCreeRien() throws Exception {
        String invalidBody = """
                {"firstName":"Awa","lastName":"Kone","email":"pas-un-email",
                 "phone":"+225 07 12 34 56 78","password":"court"}""";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(0, count);
    }
}