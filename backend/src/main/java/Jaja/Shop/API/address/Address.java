package Jaja.Shop.API.address;

import java.time.Instant;

import Jaja.Shop.API.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/*
   Adresse de livraison d'un utilisateur. Miroir de la table "addresses".
   Plusieurs adresses pour un utilisateur (@ManyToOne), dont au plus une par défaut :
   la base le garantit avec un index unique partiel (voir la migration V3).
 */

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY : l'utilisateur n'est chargé que si on y accède, pas à chaque lecture d'adresse.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "label", nullable = false, length = 50)
    private String label;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "details", nullable = false, length = 255)
    private String details;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Constructeur sans argument exigé par JPA. */
    protected Address() {
    }

    public Address(User user, String label, String city, String district, String details,
                   String phone, boolean defaultAddress) {
        this.user = user;
        this.label = label;
        this.city = city;
        this.district = district;
        this.details = details;
        this.phone = phone;
        this.defaultAddress = defaultAddress;
    }

    /* Metons à jour les informations de l'adresse. Le propriétaire ne change jamais. */
    public void update(String label, String city, String district, String details, String phone) {
        this.label = label;
        this.city = city;
        this.district = district;
        this.details = details;
        this.phone = phone;
    }

    public void markAsDefault() {
        this.defaultAddress = true;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getLabel() {
        return label;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getDetails() {
        return details;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}