package boat.carpetorgaddition.exception;

public class CarpetLoggerRegisterException extends RuntimeException {
    public CarpetLoggerRegisterException(String message, NoSuchFieldException cause) {
        super(message, cause);
    }
}
