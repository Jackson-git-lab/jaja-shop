package Jaja.Shop.API.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import Jaja.Shop.API.security.JwtAuthenticationFilter;
import Jaja.Shop.API.security.JwtService;
import Jaja.Shop.API.security.RestSecurityErrorHandler;

/*
   Règles de sécurité de l'API : qui peut appeler quelle route.
   Principes : API stateless (aucune session), authentification par token JWT,
   tout est interdit par défaut sauf ce qui est explicitement autorisé.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            RestSecurityErrorHandler errorHandler,
            CorsConfigurationSource corsConfigurationSource,
            @Value("${springdoc.swagger-ui.enabled:false}") boolean docsEnabled) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Pas de CSRF : API sans cookie de session, le token voyage dans un en-tête.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // Stateless : Spring Security ne crée ni ne lit aucune session HTTP.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 et 403 au format JSON ApiError.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                // L'ORDRE COMPTE : les règles sont lues de haut en bas, la première qui correspond gagne.
                .authorizeHttpRequests(auth -> {
                    // Routes publiques
                    auth.requestMatchers("/api/health", "/api/auth/register", "/api/auth/login").permitAll();
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/products/**", "/api/categories/**", "/api/public/**").permitAll();
                    if (docsEnabled) {
                        // La documentation n'est publique que là où elle est activée (dev, test).
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    }
                    // Administrateur seulement
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    // Client seulement
                    auth.requestMatchers("/api/cart/**", "/api/orders/**").hasRole("CLIENT");
                    // Tout utilisateur connecté (client ou administrateur)
                    auth.requestMatchers("/api/me/**").authenticated();
                    // Tout le reste est interdit sans authentification (interdit par défaut)
                    auth.anyRequest().authenticated();
                })
                // Le filtre JWT s'exécute avant le filtre d'authentification classique.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
       Spring Security n'authentifie ici personne par nom d'utilisateur : la connexion est gérée
       par AuthService, puis le token JWT prend le relais. Ce bean empêche Spring Boot de créer
       un utilisateur par défaut avec un mot de passe généré affiché dans les logs.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Seule l'authentification par token JWT est utilisée.");
        };
    }

    /* CORS : seules les origines listées dans la configuration peuvent appeler l'API. */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        if (properties.allowedOrigins().contains("*")) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins ne doit pas contenir « * » : listez les origines autorisées.");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}