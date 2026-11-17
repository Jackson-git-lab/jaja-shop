package Jaja.Shop.API.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import Jaja.Shop.API.common.error.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
   Filtre exécuté une fois par requête : s'il trouve un token JWT valide dans l'en-tête
   "Authorization: Bearer <token>", il authentifie l'utilisateur pour cette requête.
  
   Il ne refuse jamais lui-même une requête. Sans token valide, la requête continue
   sans authentification : c'est ensuite la configuration des routes qui décide
   (route publique : acceptée, route protégée : 401).
  
   Ce filtre n'est volontairement PAS un @Component : il est ajouté à la chaîne de sécurité
   dans SecurityConfig. Sinon Spring Boot l'enregistrerait aussi comme filtre classique.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                JwtClaims claims = jwtService.parse(token);
                AuthenticatedUser principal =
                        new AuthenticatedUser(claims.userId(), claims.email(), claims.role());
                // Spring Security attend le préfixe ROLE_ pour hasRole("ADMIN").
                var authority = new SimpleGrantedAuthority("ROLE_" + claims.role().name());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, List.of(authority)));
            } catch (UnauthorizedException e) {
                // Token invalide, expiré ou falsifié : on reste non authentifié.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}