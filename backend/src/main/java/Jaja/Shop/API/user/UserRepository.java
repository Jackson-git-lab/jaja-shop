package Jaja.Shop.API.user;

import org.springframework.data.jpa.repository.JpaRepository;

/*
   Accès aux utilisateurs. Spring Data génère l'implémentation à partir du nom des méthodes :
   existsByEmail devient "SELECT ... WHERE email = ?".
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}