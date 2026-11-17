package Jaja.Shop.API.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import Jaja.Shop.API.security.JwtProperties;
import Jaja.Shop.API.security.JwtService;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;

/*
   Teste les règles d'accès avec la vraie chaîne de sécurité.
   Les contrôleurs métier n'existent pas encore pour /api/admin, /api/cart, /api/me...
   On exploite donc le fait que la sécurité s'exécute AVANT le routage :
    - 401 ou 403 : la sécurité a bloqué la requête ;
    - 404 : la sécurité a laissé passer, mais la route n'existe pas encore.
   Ces tests restent valables quand les vrais contrôleurs arriveront (le chemin "anything" n'existera jamais).
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityRoutesIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String tokenFor(JwtService service, Role role) {
        User user = new User("test@example.com", "hash", "Test", "User", "+225 07 00 00 00 00", role);
        ReflectionTestUtils.setField(user, "id", 7L);
        return BEARER + service.generateToken(user);
    }

    @Test
    void lesRoutesPubliquesSontAccessiblesSansToken() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        // Catalogue et bannières : la route n'existe pas encore (404), mais la sécurité la laisse passer.
        mockMvc.perform(get("/api/products/anything")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/anything")).andExpect(status().isNotFound());
        // La connexion est publique et aucun jeton CSRF n'est exigé : un corps vide donne 400, pas 401 ni 403.
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uneRouteProtegeeSansTokenRepond401AuFormatApiError() throws Exception {
        mockMvc.perform(get("/api/me/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentification requise."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void toutCeQuiNEstPasListeEstInterditSansToken() throws Exception {
        // Interdit par défaut : une route inconnue ou une méthode non publique exige l'authentification.
        mockMvc.perform(get("/api/une-route-quelconque")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/products/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenInvalideRepond401() throws Exception {
        JwtService otherKey = new JwtService(
                new JwtProperties("une-autre-cle-de-test-9876543210-zyxwvutsrq", 15), Clock.systemUTC());

        mockMvc.perform(get("/api/me/anything").header(AUTHORIZATION, BEARER + "ceci-n-est-pas-un-jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/anything").header(AUTHORIZATION, tokenFor(otherKey, Role.CLIENT)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unTokenExpireRepond401() throws Exception {
        JwtService twoHoursAgo = new JwtService(jwtProperties,
                Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC));

        mockMvc.perform(get("/api/me/anything").header(AUTHORIZATION, tokenFor(twoHoursAgo, Role.CLIENT)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unClientAccedeAuxRoutesClientEtAuxRoutesCommunes() throws Exception {
        String token = tokenFor(jwtService, Role.CLIENT);

        // 404 = la sécurité a laissé passer (la route n'existe pas encore)
        mockMvc.perform(get("/api/cart/anything").header(AUTHORIZATION, token)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/anything").header(AUTHORIZATION, token)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/me/anything").header(AUTHORIZATION, token)).andExpect(status().isNotFound());
    }

    @Test
    void unClientNAccedePasAuxRoutesAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/anything").header(AUTHORIZATION, tokenFor(jwtService, Role.CLIENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Accès refusé : droits insuffisants."));
    }

    @Test
    void unAdminAccedeAuxRoutesAdminEtAuxRoutesCommunes() throws Exception {
        String token = tokenFor(jwtService, Role.ADMIN);

        mockMvc.perform(get("/api/admin/anything").header(AUTHORIZATION, token)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/me/anything").header(AUTHORIZATION, token)).andExpect(status().isNotFound());
    }

    @Test
    void unAdminNAccedePasAuxRoutesClient() throws Exception {
        String token = tokenFor(jwtService, Role.ADMIN);

        mockMvc.perform(get("/api/cart/anything").header(AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void aucuneSessionNEstCreee() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/me/anything")
                        .header(AUTHORIZATION, tokenFor(jwtService, Role.CLIENT)))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();

        assertNull(result.getRequest().getSession(false), "l'API doit rester stateless : aucune session");
    }

    @Test
    void lePreflightCorsAutoriseLeFrontendConfigure() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN));
    }

    @Test
    void lePreflightCorsRefuseUneOrigineInconnue() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://site-malveillant.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}