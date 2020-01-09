package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import jers.Goal;
import jers.Messages.RobotBuiltMessage;

public class DesignSchool extends Robot {

    private int landscapersBuilt;
    private boolean buildLandscaper;
    private boolean needToSendTransaction;
    private MapLocation lastLandscaperLoc = null;

    public DesignSchool(RobotController rc) {
        super(rc);
        landscapersBuilt = 0;
        buildLandscaper = true;
        needToSendTransaction = false;
        goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (buildLandscaper && landscapersBuilt < 3 && (lastLandscaperLoc = makeRobot(RobotType.LANDSCAPER)) != null) {
            buildLandscaper = false;
            needToSendTransaction = true;
            landscapersBuilt += 1;
        } else if (!buildLandscaper && checkRobotBuiltInRound(roundNum - 1, RobotType.MINER) != null) {
            buildLandscaper = true;
        }

        if (needToSendTransaction && transactor.submitTransaction(new RobotBuiltMessage(new RobotType[]{RobotType.HQ}, Goal.BUILD_LANDSCAPERS_AND_MINERS, lastLandscaperLoc, RobotType.LANDSCAPER))) {
            needToSendTransaction = false;
        }
    }
}
