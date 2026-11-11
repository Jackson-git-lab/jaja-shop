package Jaja.Shop.API.common.error;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
   Gestion centrale des erreurs : toute exception d'un contrôleur
   devient une réponse JSON au format ApiError.
  
   @RestControllerAdvice = @ControllerAdvice + @ResponseBody :
   s'applique à tous les contrôleurs, réponse en JSON.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* Nos erreurs métier : 400, 401, 403, 404, 409 selon le code porté par l'exception. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return build(ex.getErrorCode(), ex.getMessage(), ex.getDetails());
    }

    /* Un champ du corps JSON viole une contrainte (@NotBlank, @Min...). Statut 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .sorted()
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, "Les données envoyées sont invalides.", details);
    }

    /* Le JSON reçu est illisible ou mal formé. Statut 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(ErrorCode.MALFORMED_REQUEST,
                "Le corps de la requête est illisible ou mal formé.", List.of());
    }

    /* Un paramètre d'URL a un mauvais type. Statut 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(ErrorCode.INVALID_PARAMETER,
                "Le paramètre '" + ex.getName() + "' a une valeur invalide.", List.of());
    }

    /* Un paramètre obligatoire de la requête est absent. Statut 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(ErrorCode.INVALID_PARAMETER,
                "Le paramètre '" + ex.getParameterName() + "' est obligatoire.", List.of());
    }

    /* Aucune route ne correspond à l'URL demandée. Statut 404. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleRouteNotFound(Exception ex) {
        return build(ErrorCode.ROUTE_NOT_FOUND, "Route introuvable.", List.of());
    }

    /* La route existe mais pas avec cette méthode HTTP . Statut 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, "Méthode HTTP non autorisée sur cette route.", List.of());
    }

    /* Le type de contenu envoyé n'est pas supporté. Statut 415. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Type de contenu non supporté.", List.of());
    }

    /*
       Filet de sécurité : toute erreur imprévue. On garde la trace complète dans les logs du serveur,
       mais on ne renvoie JAMAIS de détail technique au client. Statut 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Erreur inattendue", ex);
        return build(ErrorCode.INTERNAL_ERROR, "Une erreur interne est survenue.", List.of());
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, List<String> details) {
        ApiError body = new ApiError(code.name(), message, details, Instant.now());
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}