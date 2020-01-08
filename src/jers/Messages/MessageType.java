package jers.Messages;

public enum MessageType {
    REFINERY_BUILT(0),
    SOUP_FOUND(1);

    private final int id;
    private MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
