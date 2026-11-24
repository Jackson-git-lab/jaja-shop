package Jaja.Shop.API.address;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Jaja.Shop.API.common.error.ConflictException;
import Jaja.Shop.API.common.error.ResourceNotFoundException;
import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.user.User;
import Jaja.Shop.API.user.UserRepository;

/* Logique métier des adresses de livraison de l'utilisateur connecté. */
@Service
public class AddressService {

    static final int MAX_ADDRESSES_PER_USER = 10;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        requireActiveUser(userId);
        return addressRepository.findAllForUser(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse get(Long userId, Long addressId) {
        requireActiveUser(userId);
        return AddressResponse.from(findOwned(userId, addressId));
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        User user = requireActiveUser(userId);

        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new ConflictException(
                    "Vous ne pouvez pas enregistrer plus de " + MAX_ADDRESSES_PER_USER + " adresses.");
        }

        // La première adresse est toujours par défaut ; les suivantes seulement si on le demande.
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.defaultAddress());
        if (makeDefault && count > 0) {
            addressRepository.clearDefault(userId);
        }

        Address address = new Address(
                user,
                request.label().trim(),
                request.city().trim(),
                request.district().trim(),
                request.details().trim(),
                request.phone().trim(),
                makeDefault);

        try {
            return AddressResponse.from(addressRepository.saveAndFlush(address));
        } catch (DataIntegrityViolationException e) {
            throw defaultAddressConflict();
        }
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        requireActiveUser(userId);
        Address address = findOwned(userId, addressId);

        address.update(
                request.label().trim(),
                request.city().trim(),
                request.district().trim(),
                request.details().trim(),
                request.phone().trim());

        // "false" ou absent ne retire jamais le statut : on change de défaut en désignant une autre adresse.
        if (Boolean.TRUE.equals(request.defaultAddress()) && !address.isDefaultAddress()) {
            addressRepository.clearDefault(userId);
            address.markAsDefault();
        }

        try {
            addressRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw defaultAddressConflict();
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        requireActiveUser(userId);
        Address address = findOwned(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);
        // La suppression doit partir en base AVANT de désigner une nouvelle adresse par défaut
        // (Hibernate envoie normalement les modifications avant les suppressions).
        addressRepository.flush();

        if (wasDefault) {
            addressRepository.findFirstByUserIdOrderByIdDesc(userId).ifPresent(Address::markAsDefault);
        }
    }

    /* Le token prouve qui est l'utilisateur, mais le compte a pu être supprimé ou désactivé depuis. */
    private User requireActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Compte introuvable ou désactivé."));
    }

    /*
       Cherche l'adresse PAR IDENTIFIANT ET PROPRIÉTAIRE. L'adresse d'un autre utilisateur donne
       exactement la même réponse qu'une adresse inexistante : on ne révèle pas son existence.
     */
    private Address findOwned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse", addressId));
    }

    private ConflictException defaultAddressConflict() {
        return new ConflictException("Conflit lors de la mise à jour de l'adresse par défaut, veuillez réessayer.");
    }
}