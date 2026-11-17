package Jaja.Shop.API.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;

/** Test unitaire du filtre JWT, sans Spring : requêtes et réponses simulées. */
class JwtAuthenticationFilterTest {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

    private final JwtService jwtService = new JwtService(
            new JwtProperties("cle-de-test-uniquement-0123456789-abcdefghij", 60),
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private String tokenFor(Role role) {
        User user = new User("awa.kone@example.com", "hash", "Awa", "Kone", "+225 07 12 34 56 78", role);
        ReflectionTestUtils.setField(user, "id", 42L);
        return jwtService.generateToken(user);
    }

    private MockFilterChain run(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    void unTokenValideAuthentifieLUtilisateurAvecSonRole() throws Exception {
        MockFilterChain chain = run("Bearer " + tokenFor(Role.ADMIN));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertEquals(42L, principal.id());
        assertEquals("awa.kone@example.com", principal.email());
        assertEquals(Role.ADMIN, principal.role());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        assertNotNull(chain.getRequest(), "la requête doit continuer dans la chaîne");
    }

    @Test
    void unTokenInvalideLaisseLaRequeteNonAuthentifieeMaisLaLaisseContinuer() throws Exception {
        MockFilterChain chain = run("Bearer ceci-n-est-pas-un-jwt");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void sansEnTeteAuthorizationLaRequeteResteNonAuthentifiee() throws Exception {
        MockFilterChain chain = run(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void unAutreSchemaQueBearerEstIgnore() throws Exception {
        run("Basic dXNlcjpwYXNz");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}