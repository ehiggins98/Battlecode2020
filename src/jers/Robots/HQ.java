package jers.Robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import static jers.Constants.directions;

public class HQ extends Robot {
    static final int MIN_SOUP_FOR_MINER = 400;
    static final int MIN_ROUNDS_BETWEEN_MINERS = 100;
    private int lastMinerBuilt = 0;

    public HQ(RobotController rc) throws GameActionException {
        super(rc);

        makeMiner();
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (rc.getTeamSoup() >= MIN_SOUP_FOR_MINER && roundNum - lastMinerBuilt > MIN_ROUNDS_BETWEEN_MINERS) {
            if(makeMiner()) {
                lastMinerBuilt = roundNum;
            }
        }
    }

    private boolean makeMiner() throws GameActionException {
        for (Direction d : directions) {
            if (rc.canBuildRobot(RobotType.MINER, d)) {
                rc.buildRobot(RobotType.MINER, d);
                return true;
            }
        }

        return false;
    }
}
