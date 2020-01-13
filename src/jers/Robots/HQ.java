package jers.Robots;

import battlecode.common.*;
import jers.Goal;
import jers.Messages.ChangeGoalMessage;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RobotBuiltMessage;

import java.util.ArrayList;

public class HQ extends Robot {
    private final int INITIAL_MINER_COUNT = 5;
    private final int SECOND_MINER_COUNT = 12;
    private boolean buildMiner = true;
    private boolean locationBroadcast = false;
    private int minersBuilt;
    private RobotBuiltMessage robotBuiltMessage;
    private boolean hasStartedSecondAttack;


    public HQ(RobotController rc) throws GameActionException {
        super(rc);
        minersBuilt = 0;
        goal = Goal.BUILD_INITIAL_MINERS;
        myHQ = rc.getLocation();
        hasStartedSecondAttack = false;
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (!locationBroadcast && transactor.submitTransaction(new RobotBuiltMessage(RobotType.values(), Goal.ALL, rc.getLocation(), RobotType.HQ))) {
            locationBroadcast = true;
        }

        switch (goal) {
            case BUILD_INITIAL_MINERS:
                buildInitialMiners();
                break;
            case BUILD_LANDSCAPERS_AND_MINERS:
                buildLandscapersAndMiners(roundNum);
                break;
            case BUILD_LANDSCAPERS_AND_DRONES:
                buildLandscapersAndDrones(roundNum);
                break;
            case REQUEST_LANDSCAPERS_AND_DRONES:
                requestLandscapersAndDrones(roundNum);
                break;
            default:
                throw new IllegalStateException("Invalid goal for HQ: " + goal);
        }
    }

    private void buildInitialMiners() throws GameActionException {
        if (rc.isReady() && rc.getTeamSoup() > RobotType.MINER.cost) {
            makeRobot(RobotType.MINER);
            minersBuilt += 1;
        }

        if (minersBuilt >= INITIAL_MINER_COUNT) {
            goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
        }
    }

    private void buildLandscapersAndMiners(int roundNum) throws GameActionException {
        if (buildMiner) {
            MapLocation builtAt = makeRobot(RobotType.MINER);
            if (builtAt == null) {
                return;
            }

            buildMiner = false;
            minersBuilt += 1;
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.DESIGN_SCHOOL}, Goal.BUILD_LANDSCAPERS_AND_MINERS, builtAt, RobotType.MINER);
        } else if (checkRobotBuiltInRound(roundNum - 1, RobotType.LANDSCAPER) != null) {
            buildMiner = true;
        }

        if (robotBuiltMessage != null && transactor.submitTransaction(robotBuiltMessage)) {
            robotBuiltMessage = null;
        }

        if (minersBuilt >= SECOND_MINER_COUNT-1) {
            System.out.println("Set new goal");
            goal = Goal.REQUEST_LANDSCAPERS_AND_DRONES;
        }
    }

    private void requestLandscapersAndDrones(int roundNum) throws GameActionException {
        int numCompleted = 0;
        ArrayList<Message> messages = transactor.getBlock(roundNum - 1, goal);
        for (Message message : messages) {
            if (message.getMessageType().equals(MessageType.REQUEST_COMPLETED)) {
                numCompleted += 1;
            }
        }
        if (numCompleted == 2) {
            goal = Goal.BUILD_LANDSCAPERS_AND_DRONES;
            return;
        }
        transactor.submitTransaction(new ChangeGoalMessage(new RobotType[] {RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER}, Goal.IDLE, Goal.WAITING_FOR_COMPLETION));
    }

    private void buildLandscapersAndDrones(int roundNum) throws GameActionException {
        if (!hasStartedSecondAttack) {
            transactor.submitTransaction(new ChangeGoalMessage(new RobotType[] {RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER}, Goal.WAITING_FOR_COMPLETION, Goal.BUILD_LANDSCAPERS_AND_DRONES));
            hasStartedSecondAttack = true;
        }
    }
}
