package Jaja.Shop.API.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import Jaja.Shop.API.common.error.ConflictException;
import Jaja.Shop.API.common.error.GlobalExceptionHandler;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.UserResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String VALID_BODY = """
            {"firstName":"Awa","lastName":"Kone","email":"awa@example.com",
             "phone":"+225 07 12 34 56 78","password":"MotDePasse123"}""";

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void inscriptionValideRepond201SansMotDePasse() throws Exception {
        UserResponse created = new UserResponse(1L, "awa@example.com", "Awa", "Kone",
                "+225 07 12 34 56 78", Role.CLIENT, Instant.parse("2026-11-12T10:00:00Z"));
        when(authService.register(any(RegisterRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("awa@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void donneesInvalidesRepond400AvecLesChampsEnCause() throws Exception {
        String invalidBody = """
                {"firstName":"","lastName":"Kone","email":"pas-un-email",
                 "phone":"abc","password":"court"}""";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isNotEmpty());

        verifyNoInteractions(authService);
    }

    @Test
    void emailDejaUtiliseRepond409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Un compte existe déjà avec cet e-mail."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}