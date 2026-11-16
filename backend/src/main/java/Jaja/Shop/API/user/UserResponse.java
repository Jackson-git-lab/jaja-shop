
package Jaja.Shop.API.user;

import java.time.Instant;

/**
   Ce que l'API renvoie pour un utilisateur.
   Il n'y a ni mot de passe ni hash : on n'expose jamais l'entité User directement.
 */
public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt());
    }
}