package Jaja.Shop.API.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/*
   Accès aux utilisateurs. Spring Data génère l'implémentation à partir du nom des méthodes.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}