package Jaja.Shop.API.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.json.JsonMapper;

class RestSecurityErrorHandlerTest {

    private final RestSecurityErrorHandler handler =
            new RestSecurityErrorHandler(JsonMapper.builder().build());

    @Test
    void laRouteProtegeeSansAuthentificationRepond401AuFormatApiError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new BadCredentialsException("détail interne"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String body = response.getContentAsString();
        assertEquals("UNAUTHORIZED", JsonPath.read(body, "$.code"));
        assertEquals("Authentification requise.", JsonPath.read(body, "$.message"));
        assertTrue(body.contains("timestamp"));
    }

    @Test
    void lAccesInterditRepond403AuFormatApiError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("détail interne"));

        assertEquals(403, response.getStatus());
        String body = response.getContentAsString();
        assertEquals("FORBIDDEN", JsonPath.read(body, "$.code"));
        assertEquals("Accès refusé : droits insuffisants.", JsonPath.read(body, "$.message"));
    }
}