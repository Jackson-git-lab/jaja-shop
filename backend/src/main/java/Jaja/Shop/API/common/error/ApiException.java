package Jaja.Shop.API.common.error;

import java.util.List;

/*
   Classe de base des erreurs métier. Nos services la lancent ,
   et le GlobalExceptionHandler la transforme en réponse HTTP.
 */
public abstract class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> details;

    protected ApiException(ErrorCode errorCode, String message, List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = List.copyOf(details);
    }

    protected ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<String> getDetails() {
        return details;
    }
}