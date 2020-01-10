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
    private InitialGoalMessage initialGoalMessage;
    private RobotBuiltMessage robotBuiltMessage;

    public DesignSchool(RobotController rc) throws GameActionException {
        super(rc);
        landscapersBuilt = 0;
        buildLandscaper = true;
        goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
    }

    /**
     * Builds 7 landscapers, the first 3 of which go to attack the enemy HQ, while the remaining 4 build a wall around
     * our HQ.
     * @param roundNum
     * @throws GameActionException
     */
    @Override
    public void run(int roundNum) throws GameActionException {
        if (buildLandscaper && landscapersBuilt < LANDSCAPERS_TO_BUILD) {
            MapLocation builtAt = makeRobot(RobotType.LANDSCAPER);
            if (builtAt == null) {
                return;
            }

            buildLandscaper = false;
            landscapersBuilt += 1;
            initialGoalMessage = new InitialGoalMessage(new RobotType[]{RobotType.LANDSCAPER},
                    Goal.ALL, builtAt, roundNum, landscapersBuilt > 3 ? Goal.GO_TO_MY_HQ : Goal.FIND_ENEMY_HQ);
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.HQ},
                    Goal.BUILD_LANDSCAPERS_AND_MINERS, builtAt, RobotType.LANDSCAPER);
        } else if (!buildLandscaper && checkRobotBuiltInRound(roundNum - 1, RobotType.MINER) != null) {
            buildLandscaper = true;
        }

        if (initialGoalMessage != null && transactor.submitTransaction(initialGoalMessage)) {
            initialGoalMessage = null;
        }

        if (robotBuiltMessage != null && transactor.submitTransaction(robotBuiltMessage)) {
            robotBuiltMessage = null;
        }
    }
}
