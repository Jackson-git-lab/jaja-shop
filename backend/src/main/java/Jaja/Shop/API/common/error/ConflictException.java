package Jaja.Shop.API.common.error;

/* La demande est en conflit avec l'état actuel . Statut 409. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}