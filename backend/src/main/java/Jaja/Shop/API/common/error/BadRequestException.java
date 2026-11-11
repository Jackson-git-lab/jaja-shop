package Jaja.Shop.API.common.error;

/* La demande est invalide pour une raison métier . Statut 400. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}