package net.seansitter.mcsvr.exception;

public class InvalidCommandException extends RuntimeException {

    private static final long serialVersionUID = -1464830400709348473L;

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
