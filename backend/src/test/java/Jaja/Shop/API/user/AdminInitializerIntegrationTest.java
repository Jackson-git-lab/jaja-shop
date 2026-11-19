package Jaja.Shop.API.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import Jaja.Shop.API.security.JwtClaims;
import Jaja.Shop.API.security.JwtService;
import jakarta.validation.Validator;

/* Test de bout en bout de la création de l'administrateur initial, avec la vraie base de test. */
@SpringBootTest
@ActiveProfiles("test")
class AdminInitializerIntegrationTest {

    private static final String PASSWORD = "AdminPass123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validator validator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbcTemplate.update("DELETE FROM users");
    }

    private AdminInitializer initializerFor(String email) {
        AdminProperties properties = new AdminProperties(
                email, PASSWORD, "Admin", "Jaja Shop", "+225 00 00 00 00");
        return new AdminInitializer(userRepository, passwordEncoder, validator, properties);
    }

    private int countAdmins() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class);
    }

    @Test
    void lAdministrateurInitialPeutSeConnecterEtRecoitLeRoleAdmin() throws Exception {
        initializerFor("Admin@JajaShop.com").run(null);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ADMIN@JAJASHOP.COM\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        JwtClaims claims = jwtService.parse(JsonPath.read(response, "$.accessToken"));
        assertEquals(Role.ADMIN, claims.role());

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, "admin@jajashop.com");
        assertNotEquals(PASSWORD, hash);
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void relancerLInitialisationNeCreePasUnSecondAdministrateur() {
        AdminInitializer initializer = initializerFor("admin@jajashop.com");

        initializer.run(null);
        initializer.run(null);

        assertEquals(1, countAdmins());
    }

    @Test
    void changerLEmailConfigureNeCreePasUnSecondAdministrateur() {
        initializerFor("premier@jajashop.com").run(null);
        initializerFor("second@jajashop.com").run(null);

        assertEquals(1, countAdmins());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'premier@jajashop.com'", Integer.class));
    }

    @Test
    void unCompteClientExistantNEstJamaisPromuAdministrateur() {
        userRepository.saveAndFlush(new User("admin@jajashop.com", passwordEncoder.encode("MotDePasse123"),
                "Awa", "Kone", "+225 07 12 34 56 78", Role.CLIENT));

        assertThrows(IllegalStateException.class, () -> initializerFor("admin@jajashop.com").run(null));

        assertEquals(0, countAdmins());
        assertEquals("CLIENT", jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE email = 'admin@jajashop.com'", String.class));
    }
}