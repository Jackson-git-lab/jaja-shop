package Jaja.Shop.API.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * Test de bout en bout du profil : vraie sécurité (JWT), vraie base de test.
 * Chaque test commence avec un seul compte client, déjà inscrit.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProfileIntegrationTest {

    private static final String EMAIL = "awa.kone@example.com";
    private static final String PASSWORD = "MotDePasse123";
    private static final String AUTHORIZATION = "Authorization";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbcTemplate.update("DELETE FROM users");
        register(EMAIL, PASSWORD);
        token = login(EMAIL, PASSWORD);
    }

    private void register(String email, String password) throws Exception {
        String body = "{\"firstName\":\"Awa\",\"lastName\":\"Kone\",\"email\":\"" + email
                + "\",\"phone\":\"+225 07 12 34 56 78\",\"password\":\"" + password + "\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(response, "$.accessToken");
    }

    private String queryString(String column, String email) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM users WHERE email = ?", String.class, email);
    }

    @Test
    void sansTokenLeProfilRepond401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void consulterMonProfilRetourneMesInformationsSansMotDePasse() throws Exception {
        mockMvc.perform(get("/api/me").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value("Awa"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void modifierMonProfilEnregistreLesChangementsEtIgnoreEmailEtRole() throws Exception {
        String body = """
                {"firstName":"Aminata","lastName":"Traore","phone":"+225 05 11 22 33 44",
                 "email":"pirate@example.com","role":"ADMIN"}""";

        mockMvc.perform(put("/api/me")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Aminata"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("CLIENT"));

        assertEquals("Aminata", queryString("first_name", EMAIL));
        assertEquals("+225 05 11 22 33 44", queryString("phone", EMAIL));
        assertEquals("CLIENT", queryString("role", EMAIL));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'pirate@example.com'", Integer.class));
    }

    @Test
    void desDonneesInvalidesRepondent400EtNeModifientRien() throws Exception {
        mockMvc.perform(put("/api/me")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Traore\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(2));

        assertEquals("Awa", queryString("first_name", EMAIL));
    }

    @Test
    void changerMonMotDePasseRepond204EtLeNouveauMotDePasseFonctionne() throws Exception {
        String oldHash = queryString("password_hash", EMAIL);

        mockMvc.perform(put("/api/me/password")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NouveauPass456\"}"))
                .andExpect(status().isNoContent());

        String newHash = queryString("password_hash", EMAIL);
        assertNotEquals(oldHash, newHash);
        assertTrue(new BCryptPasswordEncoder().matches("NouveauPass456", newHash));

        // L'ancien mot de passe ne marche plus, le nouveau marche.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        login(EMAIL, "NouveauPass456");
    }

    @Test
    void unMauvaisMotDePasseActuelRepond400AvecUnCodeDedieEtNeChangeRien() throws Exception {
        String oldHash = queryString("password_hash", EMAIL);

        mockMvc.perform(put("/api/me/password")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Mauvais1234\",\"newPassword\":\"NouveauPass456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
                .andExpect(jsonPath("$.message").value("Le mot de passe actuel est incorrect."));

        assertEquals(oldHash, queryString("password_hash", EMAIL));
        login(EMAIL, PASSWORD);
    }

    @Test
    void unNouveauMotDePasseFaibleRepond400() throws Exception {
        mockMvc.perform(put("/api/me/password")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"court\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unNouveauMotDePasseIdentiqueALAncienRepond400() throws Exception {
        mockMvc.perform(put("/api/me/password")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void unAdministrateurConsulteAussiSonProfil() throws Exception {
        userRepository.saveAndFlush(new User("admin@jajashop.example", passwordEncoder.encode("AdminPass123"),
                "Admin", "Jaja Shop", "+225 00 00 00 00", Role.ADMIN));
        String adminToken = login("admin@jajashop.example", "AdminPass123");

        mockMvc.perform(get("/api/me").header(AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("admin@jajashop.example"));
    }

    @Test
    void unCompteDesactiveApresLaConnexionEstRefuseMemeAvecUnTokenEncoreValide() throws Exception {
        jdbcTemplate.update("UPDATE users SET enabled = FALSE WHERE email = ?", EMAIL);

        mockMvc.perform(get("/api/me").header(AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Compte introuvable ou désactivé."));
    }

    @Test
    void unCompteSupprimeApresLaConnexionEstRefuseMemeAvecUnTokenEncoreValide() throws Exception {
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", EMAIL);

        mockMvc.perform(get("/api/me").header(AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modifierMonProfilNAffectePasLesAutresComptes() throws Exception {
        register("autre@example.com", "AutrePass123");

        mockMvc.perform(put("/api/me")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Changé\",\"lastName\":\"Changé\",\"phone\":\"+225 05 11 22 33 44\"}"))
                .andExpect(status().isOk());

        assertEquals("Changé", queryString("first_name", EMAIL));
        assertEquals("Awa", queryString("first_name", "autre@example.com"));
    }
}