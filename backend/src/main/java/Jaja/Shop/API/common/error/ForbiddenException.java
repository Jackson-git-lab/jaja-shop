package Jaja.Shop.API.common.error;

/* L'utilisateur est connecté mais n'a pas le droit d'effectuer cette action. Statut 403. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}