package jers.Messages;

public enum MessageType {
    ROBOT_BUILT(0),
    INITIAL_GOAL(1),
    SOUP_FOUND(2),
    WATER_FOUND(3),
    CHANGE_GOAL(4),
    REQUEST_COMPLETED(5),
    LANDSCAPER_LOCATED(6);

    public final int id;
    private MessageType(int id) {
        this.id = id;
    }
}
