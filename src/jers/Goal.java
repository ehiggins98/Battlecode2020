package jers;

public enum Goal {
    IDLE(0),
    MINE(1), // Miner
    REFINE(2), // Miner
    EXPLORE(3), // Miner
    WAIT_FOR_REFINERY(4), // HQ
    BUILD_LANDSCAPERS_AND_MINERS(6), // HQ and design school
    FIND_ENEMY_HQ(7), // Landscaper
    ATTACK_ENEMY_HQ(8), // Landscaper
    GO_TO_MY_HQ(9), // Landscaper
    BUILD_HQ_WALL(10), // Landscaper
    ALL(11);

    private final int id;
    private Goal(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Goal fromId(int id) {
        for (Goal g : Goal.values()) {
            if (g.getId() == id) {
                return g;
            }
        }

        throw new IllegalArgumentException("ID does not correspond to a Goal");
    }
}
