package jers.Messages;

public enum MessageType {
    REFINERY_BUILT(0);

    private final int id;
    private MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
