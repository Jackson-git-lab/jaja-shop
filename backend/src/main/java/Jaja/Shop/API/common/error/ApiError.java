package Jaja.Shop.API.common.error;

import java.time.Instant;
import java.util.List;

/**
   Format unique de toutes les erreurs de l'API.
  
   @param code      code stable 
   @param message   message lisible pour l'utilisateur
   @param details   informations complémentaires , jamais null
   @param timestamp date et heure de l'erreur (UTC)
 */
public record ApiError(String code, String message, List<String> details, Instant timestamp) {
}