package Jaja.Shop.API.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import Jaja.Shop.API.security.JwtClaims;
import Jaja.Shop.API.security.JwtService;
import Jaja.Shop.API.user.Role;
import com.jayway.jsonpath.JsonPath;

/*
   Test de bout en bout de la connexion : vraie application, vraie base de test.
   Chaque test commence par créer un compte via l'inscription.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthLoginIntegrationTest {

    private static final String REGISTER_BODY = """
            {"firstName":"Awa","lastName":"Kone","email":"awa.kone@example.com",
             "phone":"+225 07 12 34 56 78","password":"MotDePasse123"}""";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbcTemplate.update("DELETE FROM users");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated());
    }

    private String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void connexionReussieRetourneUnTokenValideSansMotDePasse() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("AWA.KONE@EXAMPLE.COM", "MotDePasse123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("awa.kone@example.com"))
                .andExpect(jsonPath("$.user.role").value("CLIENT"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(response, "$.accessToken");
        JwtClaims claims = jwtService.parse(token);

        Long userIdInBase = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "awa.kone@example.com");
        assertEquals(userIdInBase, claims.userId());
        assertEquals("awa.kone@example.com", claims.email());
        assertEquals(Role.CLIENT, claims.role());
    }

    @Test
    void mauvaisMotDePasseRepond401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("awa.kone@example.com", "MauvaisMotDePasse1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Identifiants invalides."))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void emailInconnuRepond401AvecLeMemeMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("inconnu@example.com", "MotDePasse123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Identifiants invalides."));
    }

    @Test
    void compteDesactiveRepond401() throws Exception {
        jdbcTemplate.update("UPDATE users SET enabled = FALSE WHERE email = ?", "awa.kone@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("awa.kone@example.com", "MotDePasse123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides."));
    }

    @Test
    void corpsIncompletRepond400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"awa.kone@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}