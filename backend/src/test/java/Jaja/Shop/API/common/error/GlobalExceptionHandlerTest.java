package Jaja.Shop.API.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/*
   on vérifie que chaque type d'erreur produit le bon statut HTTP et le format ApiError.
   un faux contrôleur (ErrorTestController) lance les exceptions à la demande.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ressourceIntrouvableRepond404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Produit introuvable (identifiant : 42)."))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void conflitRepond409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Cet e-mail est déjà utilisé."));
    }

    @Test
    void nonAuthentifieRepond401() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void accesInterditRepond403() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void champsInvalidesRepond400AvecUneLigneParChamp() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void jsonMalFormeRepond400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void mauvaisTypeDeParametreRepond400() throws Exception {
        mockMvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Le paramètre 'id' a une valeur invalide."));
    }

    @Test
    void mauvaiseMethodeHttpRepond405() throws Exception {
        mockMvc.perform(post("/test/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void erreurInattendueRepond500SansRienReveler() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue."))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    /* Faux contrôleur, utilisé uniquement dans ce test. */
    @RestController
    @RequestMapping("/test")
    public static class ErrorTestController {

        @GetMapping("/not-found")
        public String notFound() {
            throw new ResourceNotFoundException("Produit", 42);
        }

        @GetMapping("/conflict")
        public String conflict() {
            throw new ConflictException("Cet e-mail est déjà utilisé.");
        }

        @GetMapping("/unauthorized")
        public String unauthorized() {
            throw new UnauthorizedException("Identifiants invalides.");
        }

        @GetMapping("/forbidden")
        public String forbidden() {
            throw new ForbiddenException("Accès interdit.");
        }

        @PostMapping("/validation")
        public String validation(@Valid @RequestBody TestRequest request) {
            return "ok";
        }

        @GetMapping("/items/{id}")
        public String item(@PathVariable int id) {
            return "ok";
        }

        @GetMapping("/boom")
        public String boom() {
            throw new IllegalStateException("secret interne : ne doit jamais sortir");
        }
    }

    public record TestRequest(@NotBlank String name, @Min(1) int quantity) {
    }
}