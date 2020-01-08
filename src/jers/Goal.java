package jers;

public enum Goal {
    IDLE(0),
    MINE(1),
    REFINE(2),
    EXPLORE(3),
    ALL(4);

    private final int id;
    private Goal(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
