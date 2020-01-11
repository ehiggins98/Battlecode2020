package jers.Messages;

public enum MessageType {
    ROBOT_BUILT(0),
    INITIAL_GOAL(1),
    SOUP_FOUND(2);

    public final int id;
    private MessageType(int id) {
        this.id = id;
    }
}
