package Jaja.Shop.API.common.error;

/* L'utilisateur n'est pas authentifié ou ses identifiants sont invalides. Statut 401. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}