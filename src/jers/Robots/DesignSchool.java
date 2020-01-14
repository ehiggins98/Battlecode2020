package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.InitialGoalMessage;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RobotBuiltMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class DesignSchool extends Robot {

    private int landscapersBuilt;
    private boolean buildLandscaper;
    private Queue<Message> messageQueue;
    private RobotType otherUnit;
    private boolean refineryNeeded = false;
    private boolean netGunBuilt = false;
    int startupLastRoundChecked = 0;

    public DesignSchool(RobotController rc) throws GameActionException {
        super(rc);
        landscapersBuilt = 0;
        buildLandscaper = true;
        goal = Goal.STARTUP;
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        otherUnit = RobotType.MINER;
        messageQueue = new LinkedList<>();
    }

    /**
     * Builds 7 landscapers, the first 3 of which go to attack the enemy HQ, while the remaining 4 build a wall around
     * our HQ.
     * @param roundNum
     * @throws GameActionException
     */
    @Override
    public void run(final int roundNum) throws GameActionException {
        if (goal == Goal.STARTUP) {
            startUp(roundNum);
            if (goal == Goal.STARTUP) {
                return;
            }
        }

        readBlockchain(roundNum);

        switch (goal) {
            case IDLE:
                break;
            case BUILD_INITIAL_LANDSCAPERS:
                buildInitialLandscapers(roundNum);
                break;
            default:
                throw new IllegalStateException("Invalid goal for design school " + goal);
        }

        writeBlockchain();
    }

    private void startUp(final int roundNum) throws GameActionException {
        if (startupLastRoundChecked >= roundNum - 2) {
            goal = Goal.BUILD_INITIAL_LANDSCAPERS;
            return;
        }

        while (startupLastRoundChecked < roundNum - 1 && Clock.getBytecodesLeft() > 600) {
            ArrayList<Message> messages = transactor.getBlock(++startupLastRoundChecked, goal);
            for (Message m : messages) {
                switch (m.getMessageType()) {
                    case ROBOT_BUILT:
                        RobotBuiltMessage robotBuiltMessage = (RobotBuiltMessage) m;
                        if (robotBuiltMessage.getRobotType() == RobotType.NET_GUN) {
                            netGunBuilt = true;
                        }
                        break;
                }
            }
        }
    }

    private void buildInitialLandscapers(final int roundNum) throws GameActionException {
        if (netGunBuilt || rc.getTeamSoup() - RobotType.LANDSCAPER.cost >= RobotType.NET_GUN.cost) {
            MapLocation builtAt = makeRobot(RobotType.LANDSCAPER);
            if (builtAt == null) {
                return;
            }

            landscapersBuilt++;
            messageQueue.add(new InitialGoalMessage(new RobotType[]{RobotType.LANDSCAPER},
                    Goal.ALL, builtAt, roundNum, Goal.GO_TO_MY_HQ));
            messageQueue.add(new RobotBuiltMessage(new RobotType[]{RobotType.MINER}, Goal.ALL, builtAt, RobotType.LANDSCAPER));
        }

        if (landscapersBuilt >= Constants.LANDSCAPERS_FOR_WALL) {
            goal = Goal.IDLE;
        }
    }

    private void readBlockchain(int roundNum) throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(roundNum - 1, goal);
        for (Message m : messages) {
            if (m.getMessageType() == MessageType.REFINERY_NEEDED) {
                refineryNeeded = true;
                break;
            }
        }
    }

    private void writeBlockchain() throws GameActionException {
        if (!messageQueue.isEmpty() && transactor.submitTransaction(messageQueue.peek())) {
            messageQueue.poll();
        }
    }
}
