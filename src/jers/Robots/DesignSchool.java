package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import jers.Goal;
import jers.Messages.InitialGoalMessage;
import jers.Messages.RobotBuiltMessage;

import static jers.Constants.INITIAL_ATTACKING_LANDSCAPERS;
import static jers.Constants.LANDSCAPERS_FOR_WALL;

public class DesignSchool extends Robot {

    private int landscapersBuilt;
    private boolean buildLandscaper;
    private InitialGoalMessage initialGoalMessage;
    private RobotBuiltMessage robotBuiltMessage;

    public DesignSchool(RobotController rc) throws GameActionException {
        super(rc);
        landscapersBuilt = 0;
        buildLandscaper = true;
        goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
    }

    /**
     * Builds 7 landscapers, the first 3 of which go to attack the enemy HQ, while the remaining 4 build a wall around
     * our HQ.
     * @param roundNum
     * @throws GameActionException
     */
    @Override
    public void run(int roundNum) throws GameActionException {
        if (buildLandscaper && landscapersBuilt < INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL) {
            MapLocation builtAt = makeRobot(RobotType.LANDSCAPER);
            if (builtAt == null) {
                return;
            }

            buildLandscaper = false;
            landscapersBuilt += 1;
            initialGoalMessage = new InitialGoalMessage(new RobotType[]{RobotType.LANDSCAPER},
                    Goal.ALL, builtAt, roundNum, landscapersBuilt > INITIAL_ATTACKING_LANDSCAPERS ? Goal.GO_TO_MY_HQ : Goal.FIND_ENEMY_HQ);
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.HQ, RobotType.MINER},
                    Goal.ALL, builtAt, RobotType.LANDSCAPER);
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
