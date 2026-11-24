package Jaja.Shop.API.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * Test de bout en bout des adresses : vraie sécurité (JWT), vraie base de test.
 * Deux utilisateurs (A et B) sont créés avant chaque test pour vérifier l'isolation entre comptes.
 */
@SpringBootTest
@ActiveProfiles("test")
class AddressIntegrationTest {

    private static final String BASE = "/api/me/addresses";
    private static final String AUTHORIZATION = "Authorization";
    private static final String EMAIL_A = "awa@example.com";
    private static final String EMAIL_B = "bintou@example.com";
    private static final String PASSWORD = "MotDePasse123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbcTemplate.update("DELETE FROM users");
        tokenA = registerAndLogin(EMAIL_A);
        tokenB = registerAndLogin(EMAIL_B);
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"" + email
                                + "\",\"phone\":\"+225 07 12 34 56 78\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(response, "$.accessToken");
    }

    private String body(String label, String defaultAddress) {
        return "{\"label\":\"" + label + "\",\"city\":\"Abidjan\",\"district\":\"Cocody\","
                + "\"details\":\"Rue 12, près de la pharmacie\",\"phone\":\"+225 07 12 34 56 78\""
                + (defaultAddress == null ? "" : ",\"defaultAddress\":" + defaultAddress) + "}";
    }

    private long createAddress(String token, String label, String defaultAddress) throws Exception {
        String response = mockMvc.perform(post(BASE)
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(label, defaultAddress)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private int defaultCount(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM addresses a JOIN users u ON a.user_id = u.id WHERE u.email = ? AND a.is_default",
                Integer.class, email);
    }

    private String defaultLabel(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT a.label FROM addresses a JOIN users u ON a.user_id = u.id WHERE u.email = ? AND a.is_default",
                String.class, email);
    }

    private int addressCount(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM addresses a JOIN users u ON a.user_id = u.id WHERE u.email = ?",
                Integer.class, email);
    }

    @Test
    void sansTokenLesAdressesRepondent401() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void laPremiereAdresseEstParDefautEtLaReponseContientLEmplacement() throws Exception {
        mockMvc.perform(post(BASE)
                        .header(AUTHORIZATION, tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Maison", null)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.label").value("Maison"))
                .andExpect(jsonPath("$.city").value("Abidjan"))
                .andExpect(jsonPath("$.defaultAddress").value(true));
    }

    @Test
    void desDonneesInvalidesRepondent400EtNeCreentRien() throws Exception {
        mockMvc.perform(post(BASE)
                        .header(AUTHORIZATION, tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"\",\"city\":\"Abidjan\",\"district\":\"Cocody\","
                                + "\"details\":\"Rue 12\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(2));

        assertEquals(0, addressCount(EMAIL_A));
    }

    @Test
    void creerUneAdresseParDefautRemplaceLAncienne() throws Exception {
        createAddress(tokenA, "Maison", null);
        createAddress(tokenA, "Bureau", "true");

        mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].label").value("Bureau"))
                .andExpect(jsonPath("$[0].defaultAddress").value(true))
                .andExpect(jsonPath("$[1].label").value("Maison"))
                .andExpect(jsonPath("$[1].defaultAddress").value(false));

        assertEquals(1, defaultCount(EMAIL_A));
    }

    @Test
    void modifierUneAdresseEnregistreLesChangementsEtPeutChangerLAdresseParDefaut() throws Exception {
        createAddress(tokenA, "Maison", null);
        long officeId = createAddress(tokenA, "Bureau", null);

        mockMvc.perform(put(BASE + "/" + officeId)
                        .header(AUTHORIZATION, tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Bureau Plateau", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Bureau Plateau"))
                .andExpect(jsonPath("$.defaultAddress").value(true));

        assertEquals(1, defaultCount(EMAIL_A));
        assertEquals("Bureau Plateau", defaultLabel(EMAIL_A));
    }

    @Test
    void demanderFalseSurLAdresseParDefautNeLuiRetirePasSonStatut() throws Exception {
        long homeId = createAddress(tokenA, "Maison", null);

        mockMvc.perform(put(BASE + "/" + homeId)
                        .header(AUTHORIZATION, tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Maison modifiée", "false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Maison modifiée"))
                .andExpect(jsonPath("$.defaultAddress").value(true));

        assertEquals(1, defaultCount(EMAIL_A));
    }

    @Test
    void supprimerLAdresseParDefautPromeutLaPlusRecenteDesRestantes() throws Exception {
        long homeId = createAddress(tokenA, "Maison", null);
        createAddress(tokenA, "Bureau", null);
        createAddress(tokenA, "Marche", null);

        mockMvc.perform(delete(BASE + "/" + homeId).header(AUTHORIZATION, tokenA))
                .andExpect(status().isNoContent());

        assertEquals(2, addressCount(EMAIL_A));
        assertEquals(1, defaultCount(EMAIL_A));
        assertEquals("Marche", defaultLabel(EMAIL_A));
    }

    @Test
    void supprimerLaDerniereAdresseLaisseUneListeVide() throws Exception {
        long homeId = createAddress(tokenA, "Maison", null);

        mockMvc.perform(delete(BASE + "/" + homeId).header(AUTHORIZATION, tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unUtilisateurNeVoitNiNeTouchePasLesAdressesDUnAutre() throws Exception {
        long addressOfA = createAddress(tokenA, "Maison", null);

        // B ne voit rien dans sa liste
        mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // B ne peut ni lire, ni modifier, ni supprimer l'adresse de A : 404, comme si elle n'existait pas
        mockMvc.perform(get(BASE + "/" + addressOfA).header(AUTHORIZATION, tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(put(BASE + "/" + addressOfA)
                        .header(AUTHORIZATION, tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Piratée", "true")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/" + addressOfA).header(AUTHORIZATION, tokenB))
                .andExpect(status().isNotFound());

        // L'adresse de A est intacte
        mockMvc.perform(get(BASE + "/" + addressOfA).header(AUTHORIZATION, tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Maison"))
                .andExpect(jsonPath("$.defaultAddress").value(true));
        assertEquals(1, addressCount(EMAIL_A));
        assertEquals(0, addressCount(EMAIL_B));
    }

    @Test
    void chaqueUtilisateurADSaPropreAdresseParDefaut() throws Exception {
        createAddress(tokenA, "Maison", null);
        createAddress(tokenB, "Bureau", null);

        assertEquals(1, defaultCount(EMAIL_A));
        assertEquals(1, defaultCount(EMAIL_B));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM addresses WHERE is_default", Integer.class));
    }

    @Test
    void ilEstImpossibleDeDepasserDixAdresses() throws Exception {
        for (int i = 1; i <= 10; i++) {
            createAddress(tokenA, "Adresse " + i, null);
        }

        mockMvc.perform(post(BASE)
                        .header(AUTHORIZATION, tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Onzième", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertEquals(10, addressCount(EMAIL_A));
    }

    @Test
    void laBaseRefuseDeuxAdressesParDefautPourUnMemeUtilisateur() throws Exception {
        createAddress(tokenA, "Maison", null);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO addresses (user_id, label, city, district, details, phone, is_default) "
                        + "SELECT id, 'Intruse', 'Abidjan', 'Cocody', 'Rue 1', '+225 07 12 34 56 78', TRUE "
                        + "FROM users WHERE email = ?", EMAIL_A));
    }

    @Test
    void supprimerUnCompteSupprimeSesAdresses() throws Exception {
        createAddress(tokenA, "Maison", null);
        createAddress(tokenB, "Bureau", null);

        jdbcTemplate.update("DELETE FROM users WHERE email = ?", EMAIL_A);

        assertEquals(0, addressCount(EMAIL_A));
        assertEquals(1, addressCount(EMAIL_B));
    }

    @Test
    void unIdentifiantNonNumeriqueRepond400() throws Exception {
        mockMvc.perform(get(BASE + "/abc").header(AUTHORIZATION, tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void unCompteDesactiveEstRefuseMemeAvecUnTokenEncoreValide() throws Exception {
        jdbcTemplate.update("UPDATE users SET enabled = FALSE WHERE email = ?", EMAIL_A);

        mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Compte introuvable ou désactivé."));
    }
}