package net.seansitter.mcsvr.exception;

/**
 * Exception class thrown when client provides invalid command
 */
public class InvalidCommandException extends RuntimeException {

    /**
     * Creates a new instance.
     */
    public InvalidCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance.
     */
    public InvalidCommandException(String message) {
        super(message);
    }


    @Override
    public String getMessage() {
        return "invalid command '"+super.getMessage()+"'";
    }
}
