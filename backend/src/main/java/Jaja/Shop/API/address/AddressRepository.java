package Jaja.Shop.API.address;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/*
   Accès aux adresses. TOUTES les requêtes sont limitées à un propriétaire (userId) :
   il n'existe volontairement aucune méthode pour chercher une adresse par son seul identifiant.
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /* Les adresses d'un utilisateur : l'adresse par défaut d'abord, puis par ordre de création. */
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId ORDER BY a.defaultAddress DESC, a.id ASC")
    List<Address> findAllForUser(@Param("userId") Long userId);

    /* Retrouve une adresse SEULEMENT si elle appartient à cet utilisateur. */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    /* La plus récente des adresses d'un utilisateur (sert à désigner une nouvelle adresse par défaut). */
    Optional<Address> findFirstByUserIdOrderByIdDesc(Long userId);

    /*
       Retire le statut "par défaut" de l'adresse par défaut actuelle de l'utilisateur.
       Exécuté immédiatement en SQL, AVANT d'enregistrer la nouvelle adresse par défaut :
       sinon l'index unique partiel refuserait d'avoir deux adresses par défaut, même brièvement.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.user.id = :userId AND a.defaultAddress = true")
    int clearDefault(@Param("userId") Long userId);
}