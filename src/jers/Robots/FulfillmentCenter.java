package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import jers.Goal;
import jers.Messages.InitialGoalMessage;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RobotBuiltMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import static jers.Constants.*;

public class FulfillmentCenter extends Robot {

    private int dronesBuilt;
    private boolean buildDrone;
    private Queue<Message> messageQueue;
    private boolean refineryNeeded = true;
    private int roundsWithSoup = 0;
    private boolean netGunBuilt = false;

    public FulfillmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        dronesBuilt = 0;
        buildDrone = true;
        goal = Goal.BUILD_INITIAL_DRONES;
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        messageQueue = new LinkedList<>();
    }


    @Override
    public void run(int roundNum) throws GameActionException {
        readBlockchain(roundNum);

        switch (goal) {
            case IDLE:
                break;
            case BUILD_INITIAL_DRONES:
                buildInitialDrones(roundNum);
                break;
            default:
                throw new IllegalStateException("Invalid goal for Fulfillment Center: " + goal);
        }

        writeBlockchain();
    }
    /**
     * Build 4 drones that will go harass enemy, then wait until second wave starts
     * @param roundNum
     * @throws GameActionException
     */
    public void buildInitialDrones(int roundNum) throws GameActionException {
        if (netGunBuilt || rc.getTeamSoup() - RobotType.DELIVERY_DRONE.cost >= RobotType.NET_GUN.cost) {
            MapLocation builtAt = makeRobot(RobotType.DELIVERY_DRONE);
            if (builtAt == null) {
                return;
            }
            dronesBuilt += 1;
            messageQueue.add(new InitialGoalMessage(new RobotType[]{RobotType.DELIVERY_DRONE},
                    Goal.ALL, builtAt, roundNum, Goal.GO_TO_MY_HQ));
            messageQueue.add(new RobotBuiltMessage(new RobotType[]{RobotType.MINER}, Goal.ALL, builtAt, RobotType.DELIVERY_DRONE));
        }

        if (dronesBuilt >= INITIAL_DEFENDING_DRONES) {
            goal = Goal.IDLE;
            buildDrone = true;
        }
    }
    /**
     * Build 4 landscaper-drone pairs. Drones will pick up landscapers and drop them near enemy
     * base to start piling on dirt.
     * @param roundNum
     * @throws GameActionException
     */
    public void buildLandscapersAndDrones(int roundNum) throws GameActionException {
        if (buildDrone) {
            MapLocation builtAt = makeRobot(RobotType.DELIVERY_DRONE);
            if (builtAt == null) {
                return;
            }

            buildDrone = false;
            dronesBuilt += 1;
            messageQueue.add(new InitialGoalMessage(new RobotType[]{RobotType.DELIVERY_DRONE},
                    Goal.ALL, builtAt, roundNum, Goal.FIND_ENEMY_HQ));
            messageQueue.add(new RobotBuiltMessage(new RobotType[]{RobotType.HQ, RobotType.DESIGN_SCHOOL},
                    Goal.ALL, builtAt, RobotType.DELIVERY_DRONE));
        } else if (checkRobotBuiltInRound(roundNum - 1, RobotType.LANDSCAPER) != null) {
            buildDrone = true;
        }
    }

    private void readBlockchain(int roundNum) throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(roundNum - 1, goal);
        for (Message m : messages) {
            switch (m.getMessageType()) {
                case REFINERY_NEEDED:
                    break;
                case ROBOT_BUILT:
                    RobotBuiltMessage robotBuiltMessage = (RobotBuiltMessage) m;
                    if (robotBuiltMessage.getRobotType() == RobotType.NET_GUN) {
                        netGunBuilt = true;
                    }
                    break;
            }
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
