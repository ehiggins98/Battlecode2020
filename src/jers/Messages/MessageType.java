package jers.Messages;

public enum MessageType {
    ROBOT_BUILT(0),
    INITIAL_GOAL(1),
    SOUP_FOUND(2),
    WATER_FOUND(3),
    UNIT_PICKED_UP(4),
    HQ_FOUND(5),
    REFINERY_NEEDED(6);

    public final int id;
    private MessageType(int id) {
        this.id = id;
    }
}
