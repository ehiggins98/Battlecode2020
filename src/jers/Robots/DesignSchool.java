package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import jers.Constants;
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
    private RobotType otherUnit;

    public DesignSchool(RobotController rc) throws GameActionException {
        super(rc);
        landscapersBuilt = 0;
        buildLandscaper = true;
        goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        otherUnit = RobotType.MINER;
    }

    /**
     * Builds 7 landscapers, the first 3 of which go to attack the enemy HQ, while the remaining 4 build a wall around
     * our HQ.
     * @param roundNum
     * @throws GameActionException
     */
    @Override
    public void run(int roundNum) throws GameActionException {
        if (landscapersBuilt == INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL) {
            otherUnit = RobotType.DELIVERY_DRONE;
        }

        buildLandscapersAndOtherUnit(otherUnit, roundNum);
    }

    private void buildLandscapersAndOtherUnit(RobotType otherUnit, int roundNum) throws GameActionException {
        System.out.println(buildLandscaper);
        if (buildLandscaper) {
            MapLocation builtAt = makeRobot(RobotType.LANDSCAPER);
            if (builtAt == null) {
                return;
            }

            Goal initialGoal;
            if (landscapersBuilt < INITIAL_ATTACKING_LANDSCAPERS) {
                initialGoal = Goal.FIND_ENEMY_HQ;
            } else if (landscapersBuilt < INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL) {
                initialGoal = Goal.GO_TO_MY_HQ;
            } else {
                initialGoal = Goal.FIND_ENEMY_HQ;
            }

            buildLandscaper = false;
            landscapersBuilt += 1;
            initialGoalMessage = new InitialGoalMessage(new RobotType[]{RobotType.LANDSCAPER},
                    Goal.ALL, builtAt, roundNum, initialGoal);
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.HQ, RobotType.MINER, RobotType.FULFILLMENT_CENTER},
                    Goal.ALL, builtAt, RobotType.LANDSCAPER);

            if (landscapersBuilt >= INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL) {
                goal = Goal.BUILD_LANDSCAPERS_AND_DRONES;
            }
        } else if (checkRobotBuiltInRound(roundNum - 1, otherUnit) != null) {
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
