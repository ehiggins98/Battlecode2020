package jers;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
            Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static Team team;
    static final int MIN_SOUP_FOR_MINER = 400;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        RobotPlayer.team = rc.getTeam();
        System.out.println("I'm a " + rc.getType().toString());

        PathFinder pf = null;
        if (rc.getType() == RobotType.MINER) {
            pf = new PathFinder(rc, rc.getLocation(), new MapLocation(0, 0));
        } else if (rc.getType() == RobotType.HQ) {
            makeMiner();
        }

        while (true) {
            turnCount += 1;

            try {
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner(pf);
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                }

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }

            Clock.yield();
        }
    }

    static void runHQ() throws GameActionException {
        if (rc.getTeamSoup() >= MIN_SOUP_FOR_MINER) {
            makeMiner();
        }
    }

    static void runMiner(PathFinder pf) throws GameActionException {
        if (!pf.isFinished()) {
            pf.move();
        }
    }

    static void runRefinery() {

    }

    private static void makeMiner() throws GameActionException {
        for (Direction d : directions) {
            if (rc.canBuildRobot(RobotType.MINER, d)) {
                rc.buildRobot(RobotType.MINER, d);
                return;
            }
        }
    }
}
