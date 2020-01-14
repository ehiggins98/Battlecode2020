package jers;

public enum Goal {
    IDLE(0),
    MINE(1), // Miner
    REFINE(2), // Miner
    EXPLORE(3), // Miner
    BUILD_INITIAL_MINERS(4), // HQ
    BUILD_LANDSCAPERS_AND_MINERS(6), // HQ and design school
    FIND_ENEMY_HQ(7), // Landscaper
    ATTACK_ENEMY_HQ(8), // Landscaper
    GO_TO_MY_HQ(9), // Landscaper
    BUILD_HQ_WALL(10), // Landscaper
    BUILD_REFINERY(11), // Miner
    STARTUP(12), // Miner, Drone
    BUILD_LANDSCAPERS_AND_DRONES(13), // HQ, Design school, and Fulfillment Center
    BUILD_INITIAL_DRONES(14), // Fulfillment Center
    ATTACK_UNITS(15), // Drone
    DESTROY_UNIT(17), // Drone
    PICK_UP_UNIT(18), // Drone
    GO_TO_ENEMY_HQ(19), // Drone
    FIND_WATER(20), // Drone
    GET_INITIAL_GOAL(21), // Drone
    ROAM_AROUND(22), // Drone
    BUILD_NET_GUN(23), // Miner
    FIND_NEW_SOUP(24), // Miner
    BUILD_DESIGN_SCHOOL(25), // Miner
    BUILD_FULFILLMENT_CENTER(26), //Miner
    ALL(27);

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
