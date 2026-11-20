package Jaja.Shop.API.common.error;

/*
   Le mot de passe actuel fourni n'est pas le bon.
   Statut 400 (et pas 401) : le token est valide, c'est la saisie qui est fausse.
   Un 401 ferait croire au frontend que la session a expiré.
 */
public class InvalidCurrentPasswordException extends ApiException {

    public InvalidCurrentPasswordException() {
        super(ErrorCode.INVALID_CURRENT_PASSWORD, "Le mot de passe actuel est incorrect.");
    }
}