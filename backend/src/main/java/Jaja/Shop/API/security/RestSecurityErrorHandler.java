package Jaja.Shop.API.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import Jaja.Shop.API.common.error.ApiError;
import Jaja.Shop.API.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/*
   Réponses d'erreur de la chaîne de sécurité, au même format JSON que le reste de l'API.
   - commence() : appelée quand une route protégée est demandée sans authentification valide (401)
   - handle()   : appelée quand l'utilisateur est connecté mais n'a pas le bon rôle (403)
   Ces erreurs naissent AVANT les contrôleurs : le @RestControllerAdvice ne les voit pas.
 */
@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public RestSecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED, "Authentification requise.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.FORBIDDEN, "Accès refusé : droits insuffisants.");
    }

    private void write(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        ApiError body = new ApiError(code.name(), message, List.of(), Instant.now());
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}