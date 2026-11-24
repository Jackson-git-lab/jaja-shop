package Jaja.Shop.API.address;

import java.time.Instant;

/* Ce que l'API renvoie pour une adresse. */
public record AddressResponse(
        Long id,
        String label,
        String city,
        String district,
        String details,
        String phone,
        boolean defaultAddress,
        Instant createdAt
) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getCity(),
                address.getDistrict(),
                address.getDetails(),
                address.getPhone(),
                address.isDefaultAddress(),
                address.getCreatedAt());
    }
}