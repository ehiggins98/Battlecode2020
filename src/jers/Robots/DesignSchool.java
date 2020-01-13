package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import jers.Goal;
import jers.Messages.*;
import jers.Messages.InitialGoalMessage;
import jers.Messages.RobotBuiltMessage;

import java.util.ArrayList;

import static jers.Constants.*;

public class DesignSchool extends Robot {

    private int landscapersBuilt;
    private boolean buildLandscaper;
    private InitialGoalMessage initialGoalMessage;
    private RobotBuiltMessage robotBuiltMessage;
    private boolean hasStartedSecondAttack;
    private int roundLastUpdated;

    public DesignSchool(RobotController rc) throws GameActionException {
        super(rc);
        landscapersBuilt = 0;
        buildLandscaper = true;
        goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        hasStartedSecondAttack = false;
        roundLastUpdated = rc.getRoundNum();
    }


    @Override
    public void run(int roundNum) throws GameActionException {
        goal = getGoalFromMessage();
        Goal lastGoal = null;
        while (rc.isReady() && lastGoal != goal) {
            lastGoal = goal;
            switch (goal) {
                case IDLE:
                    break;
                case WAITING_FOR_COMPLETION:
                    transactor.submitTransaction(new RequestCompletedMessage(new RobotType[]{RobotType.HQ}, Goal.ALL));
                    break;
                case BUILD_LANDSCAPERS_AND_MINERS:
                    buildLandscapersAndMiners(roundNum);
                    break;
                case BUILD_LANDSCAPERS_AND_DRONES:
                    buildLandscapersAndDrones(roundNum);
                    break;
                default:
                    throw new IllegalStateException("Invalid goal for Design School: " + goal);
            }
        }
    }
    /**
     * Builds 7 landscapers, the first 3 of which go to attack the enemy HQ, while the remaining 4 build a wall around
     * our HQ.
     * @param roundNum
     * @throws GameActionException
     */
    public void buildLandscapersAndMiners(int roundNum) throws GameActionException {
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

        if (landscapersBuilt > INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL - 1) {
            goal = Goal.IDLE;
            roundLastUpdated = rc.getRoundNum();
        }
    }

    public void buildLandscapersAndDrones(int roundNum) throws GameActionException {
        buildLandscaper = wasDroneMade(roundNum);
        if (!hasStartedSecondAttack) {
            buildLandscaper = true;
            hasStartedSecondAttack = true;
        }
        if (buildLandscaper && landscapersBuilt < INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL + DRONE_LANDSCAPER_PAIRS) {
            MapLocation builtAt = makeRobot(RobotType.LANDSCAPER);
            if (builtAt == null) {
                return;
            }

            System.out.println("Built landscaper for second wave");
            buildLandscaper = false;
            landscapersBuilt += 1;
            initialGoalMessage = new InitialGoalMessage(new RobotType[]{RobotType.LANDSCAPER},
                    Goal.ALL, builtAt, roundNum, Goal.IDLE);
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.HQ, RobotType.FULFILLMENT_CENTER},
                    Goal.ALL, builtAt, RobotType.LANDSCAPER);
        }

        if (initialGoalMessage != null && transactor.submitTransaction(initialGoalMessage)) {
            initialGoalMessage = null;
        }

        if (robotBuiltMessage != null && transactor.submitTransaction(robotBuiltMessage)) {
            robotBuiltMessage = null;
        }
    }

    public boolean wasDroneMade(int roundNum) throws GameActionException {
        for (int i = roundLastUpdated-1; i < roundNum; i++) {
            ArrayList<Message> messages = transactor.getBlock(i, goal);
            for (Message message : messages) {
                if (message.getMessageType().equals(MessageType.ROBOT_BUILT)) {
                    if (((RobotBuiltMessage) message).getRobotType() == RobotType.DELIVERY_DRONE) {
                        System.out.println("It was good");
                        roundLastUpdated = roundNum;
                        return true;
                    }
                }
            }
        }


        return buildLandscaper;
    }

    public Goal getGoalFromMessage() throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(rc.getRoundNum() - 1, goal);
        for (Message message : messages) {
            if (message.getMessageType().equals(MessageType.CHANGE_GOAL)) {
                return ((ChangeGoalMessage) message).getChangeGoalTo();
            }
        }

        return goal;
    }
}
