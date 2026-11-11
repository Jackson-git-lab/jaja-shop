package Jaja.Shop.API.common.error;

/*  La ressource demandée n'existe pas. Statut 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        this(resource + " introuvable (identifiant : " + id + ").");
    }
}