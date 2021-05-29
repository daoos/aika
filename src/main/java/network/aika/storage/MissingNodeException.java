package network.aika.storage;

public class MissingNodeException extends RuntimeException {

    public MissingNodeException(String message) {
        super(message);
    }
}