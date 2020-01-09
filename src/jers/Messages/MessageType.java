package jers.Messages;

public enum MessageType {
    ROBOT_BUILT(0),
    INITIAL_GOAL(1);

    private final int id;
    private MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
