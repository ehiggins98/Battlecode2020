package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import jers.Goal;
import jers.Messages.InitialGoalMessage;
import jers.Messages.RobotBuiltMessage;

public class DesignSchool extends Robot {

    private final int LANDSCAPERS_TO_BUILD = 7; // 3 offensive and 4 defensive
    private int landscapersBuilt;
    private boolean buildLandscaper;
    private boolean sendLandscaperCreatedMsg = false;
    private Goal initialGoal = null;
    private MapLocation lastLandscaperLoc = null;
    private int lastCreatedOnRound;

    public DesignSchool(RobotController rc) throws GameActionException {
        super(rc);
        landscapersBuilt = 0;
        buildLandscaper = true;
        goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (buildLandscaper && landscapersBuilt < LANDSCAPERS_TO_BUILD && (lastLandscaperLoc = makeRobot(RobotType.LANDSCAPER)) != null) {
            buildLandscaper = false;
            landscapersBuilt += 1;
            lastCreatedOnRound = roundNum;
            sendLandscaperCreatedMsg = true;
            initialGoal = landscapersBuilt > 3 ? Goal.GO_TO_MY_HQ : Goal.FIND_ENEMY_HQ;
        } else if (!buildLandscaper && checkRobotBuiltInRound(roundNum - 1, RobotType.MINER) != null) {
            buildLandscaper = true;
        }

        if (initialGoal != null &&
                transactor.submitTransaction(new InitialGoalMessage(new RobotType[]{RobotType.LANDSCAPER},
                        Goal.ALL, lastLandscaperLoc, lastCreatedOnRound, initialGoal))) {
            initialGoal = null;
        }

        if (sendLandscaperCreatedMsg &&
                transactor.submitTransaction(new RobotBuiltMessage(new RobotType[]{RobotType.HQ},
                        Goal.BUILD_LANDSCAPERS_AND_MINERS, lastLandscaperLoc, RobotType.LANDSCAPER))) {
            sendLandscaperCreatedMsg = false;
        }
    }
}
