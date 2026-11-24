package Jaja.Shop.API.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import Jaja.Shop.API.common.error.ConflictException;
import Jaja.Shop.API.common.error.ResourceNotFoundException;
import Jaja.Shop.API.common.error.UnauthorizedException;
import Jaja.Shop.API.user.Role;
import Jaja.Shop.API.user.User;
import Jaja.Shop.API.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User givenActiveUser() {
        User user = new User("awa@example.com", "hash", "Awa", "Kone", "+225 07 12 34 56 78", Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    private Address address(User owner, Long id, boolean defaultAddress) {
        Address address = new Address(owner, "Maison", "Abidjan", "Cocody",
                "Rue 12, près de la pharmacie", "+225 07 12 34 56 78", defaultAddress);
        ReflectionTestUtils.setField(address, "id", id);
        return address;
    }

    private AddressRequest request(Boolean defaultAddress) {
        return new AddressRequest("  Bureau ", " Abidjan", "Plateau ", "Immeuble Alpha, 3e étage",
                "+225 05 11 22 33 44", defaultAddress);
    }

    @Test
    void laPremiereAdresseDevientParDefautEtLesChampsSontNettoyes() {
        givenActiveUser();
        when(addressRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.create(USER_ID, request(null));

        assertTrue(response.defaultAddress());
        assertEquals("Bureau", response.label());
        assertEquals("Plateau", response.district());
        verify(addressRepository, never()).clearDefault(any());
    }

    @Test
    void uneAdresseSupplementaireNEstPasParDefautSansLeDemander() {
        givenActiveUser();
        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.create(USER_ID, request(null));

        assertFalse(response.defaultAddress());
        verify(addressRepository, never()).clearDefault(any());
    }

    @Test
    void demanderLeStatutParDefautRetireLAncienneAvantDEnregistrer() {
        givenActiveUser();
        when(addressRepository.countByUserId(USER_ID)).thenReturn(2L);
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.create(USER_ID, request(true));

        assertTrue(response.defaultAddress());
        InOrder inOrder = inOrder(addressRepository);
        inOrder.verify(addressRepository).clearDefault(USER_ID);
        inOrder.verify(addressRepository).saveAndFlush(any(Address.class));
    }

    @Test
    void refuseDeDepasserLeNombreMaximalDAdresses() {
        givenActiveUser();
        when(addressRepository.countByUserId(USER_ID)).thenReturn(10L);

        assertThrows(ConflictException.class, () -> addressService.create(USER_ID, request(null)));

        verify(addressRepository, never()).saveAndFlush(any());
    }

    @Test
    void transformeUnConflitDeConcurrenceEn409() {
        givenActiveUser();
        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(addressRepository.saveAndFlush(any(Address.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(ConflictException.class, () -> addressService.create(USER_ID, request(true)));
    }

    @Test
    void listeRetourneLesAdressesDeLUtilisateur() {
        User user = givenActiveUser();
        when(addressRepository.findAllForUser(USER_ID))
                .thenReturn(List.of(address(user, 2L, true), address(user, 1L, false)));

        List<AddressResponse> responses = addressService.list(USER_ID);

        assertEquals(2, responses.size());
        assertTrue(responses.get(0).defaultAddress());
    }

    @Test
    void lireUneAdresseQuiNAppartientPasAUtilisateurDonne404() {
        givenActiveUser();
        when(addressRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.get(USER_ID, 99L));
    }

    @Test
    void modifierUneAdresseChangeSesChampsEtPeutLaRendreParDefaut() {
        User user = givenActiveUser();
        Address address = address(user, 5L, false);
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(address));

        AddressResponse response = addressService.update(USER_ID, 5L, request(true));

        assertEquals("Bureau", address.getLabel());
        assertEquals("Plateau", address.getDistrict());
        assertEquals("+225 05 11 22 33 44", address.getPhone());
        assertTrue(address.isDefaultAddress());
        assertTrue(response.defaultAddress());
        verify(addressRepository).clearDefault(USER_ID);
    }

    @Test
    void demanderFalseSurLAdresseParDefautNeLuiRetirePasSonStatut() {
        User user = givenActiveUser();
        Address address = address(user, 5L, true);
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(address));

        AddressResponse response = addressService.update(USER_ID, 5L, request(false));

        assertTrue(address.isDefaultAddress());
        assertTrue(response.defaultAddress());
        verify(addressRepository, never()).clearDefault(any());
    }

    @Test
    void modifierUneAdresseDUnAutreDonne404EtNeToucheAucunStatut() {
        givenActiveUser();
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.update(USER_ID, 5L, request(true)));

        verify(addressRepository, never()).clearDefault(any());
    }

    @Test
    void supprimerUneAdresseQuiNEstPasParDefautNePromeutPersonne() {
        User user = givenActiveUser();
        Address address = address(user, 5L, false);
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(address));

        addressService.delete(USER_ID, 5L);

        verify(addressRepository).delete(address);
        verify(addressRepository, never()).findFirstByUserIdOrderByIdDesc(any());
    }

    @Test
    void supprimerLAdresseParDefautPromeutLaPlusRecenteDesRestantes() {
        User user = givenActiveUser();
        Address deleted = address(user, 5L, true);
        Address remaining = address(user, 3L, false);
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(deleted));
        when(addressRepository.findFirstByUserIdOrderByIdDesc(USER_ID)).thenReturn(Optional.of(remaining));

        addressService.delete(USER_ID, 5L);

        InOrder inOrder = inOrder(addressRepository);
        inOrder.verify(addressRepository).delete(deleted);
        inOrder.verify(addressRepository).flush();
        inOrder.verify(addressRepository).findFirstByUserIdOrderByIdDesc(USER_ID);
        assertTrue(remaining.isDefaultAddress());
    }

    @Test
    void supprimerUneAdresseDUnAutreDonne404EtNeSupprimeRien() {
        givenActiveUser();
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.delete(USER_ID, 5L));

        verify(addressRepository, never()).delete(any());
    }

    @Test
    void unCompteDesactiveEstRefuseSansInterrogerLesAdresses() {
        User user = givenActiveUser();
        ReflectionTestUtils.setField(user, "enabled", false);

        assertThrows(UnauthorizedException.class, () -> addressService.list(USER_ID));

        verifyNoInteractions(addressRepository);
    }
}