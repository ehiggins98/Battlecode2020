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

public class FulfillmentCenter extends Robot {

    private int dronesBuilt;
    private boolean buildDrone;
    private InitialGoalMessage initialGoalMessage;
    private RobotBuiltMessage robotBuiltMessage;
    private LandscaperLocationMessage landscaperLocatedMessage;
    private MapLocation locationOfLandscaper = null;
    private int roundLastUpdated;

    public FulfillmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        dronesBuilt = 0;
        buildDrone = true;
        goal = Goal.BUILD_INITIAL_DRONES;
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        roundLastUpdated = rc.getRoundNum();
    }


    @Override
    public void run(int roundNum) throws GameActionException {
        goal = getGoalFromMessage();
        switch (goal) {
            case IDLE:
                break;
            case WAITING_FOR_COMPLETION:
                transactor.submitTransaction(new RequestCompletedMessage(new RobotType[] {RobotType.HQ}, Goal.ALL));
                break;
            case BUILD_INITIAL_DRONES:
                buildInitialDrones(roundNum);
                break;
            case BUILD_LANDSCAPERS_AND_DRONES:
                buildLandscapersAndDrones(roundNum);
                break;
            default:
                throw new IllegalStateException("Invalid goal for Fulfillment Center: " + goal);
        }
    }
    /**
     * Build 4 drones that will go harass enemy, then wait until second wave starts
     * @param roundNum
     * @throws GameActionException
     */
    public void buildInitialDrones(int roundNum) throws GameActionException {

        if (rc.isReady() && rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost) {
            MapLocation builtAt = makeRobot(RobotType.DELIVERY_DRONE);
            if (builtAt == null) {
                return;
            }

            dronesBuilt += 1;
            initialGoalMessage = new InitialGoalMessage(new RobotType[]{RobotType.DELIVERY_DRONE},
                    Goal.ALL, builtAt, roundNum, dronesBuilt > INITIAL_ATTACKING_DRONES ? Goal.FIND_ENEMY_HQ : Goal.GO_TO_MY_HQ);
        }

        if (initialGoalMessage != null && transactor.submitTransaction(initialGoalMessage)) {
            System.out.println(("Told a drone to find the base"));
            initialGoalMessage = null;
        }

        if (dronesBuilt > INITIAL_ATTACKING_DRONES + INITIAL_DEFENDING_DRONES - 1) {
            goal = Goal.IDLE;
            roundLastUpdated = rc.getRoundNum();
            buildDrone = false;
        }
    }
    /**
     * Build 4 landscaper-drone pairs. Drones will pick up landscapers and drop them near enemy
     * base to start piling on dirt.
     * @param roundNum
     * @throws GameActionException
     */
    public void buildLandscapersAndDrones(int roundNum) throws GameActionException {
        locationOfLandscaper = lookForLandscaper(roundNum);
        if (locationOfLandscaper != null && dronesBuilt < INITIAL_ATTACKING_DRONES + INITIAL_DEFENDING_DRONES + DRONE_LANDSCAPER_PAIRS) {
            MapLocation builtAt = makeRobot(RobotType.DELIVERY_DRONE);
            if (builtAt == null) {
                return;
            }

            System.out.println("Built drone for second wave");
            dronesBuilt += 1;
            initialGoalMessage = new InitialGoalMessage(new RobotType[]{RobotType.DELIVERY_DRONE},
                    Goal.ALL, builtAt, roundNum, Goal.PICK_UP_LANDSCAPER);
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.HQ, RobotType.DESIGN_SCHOOL},
                    Goal.ALL, builtAt, RobotType.DELIVERY_DRONE);
            landscaperLocatedMessage = new LandscaperLocationMessage(new RobotType[]{RobotType.DELIVERY_DRONE},
                    Goal.PICK_UP_LANDSCAPER, locationOfLandscaper);
            locationOfLandscaper = null;
        }

        if (initialGoalMessage != null && transactor.submitTransaction(initialGoalMessage)) {
            initialGoalMessage = null;
        }

        if (robotBuiltMessage != null && transactor.submitTransaction(robotBuiltMessage)) {
            robotBuiltMessage = null;
        }

        if (landscaperLocatedMessage != null && transactor.submitTransaction(landscaperLocatedMessage)) {
            landscaperLocatedMessage = null;
        }

    }

    public MapLocation lookForLandscaper(int roundNum) throws GameActionException {
        for (int i = roundLastUpdated; i < roundNum; i++) {
            ArrayList<Message> messages = transactor.getBlock(i, goal);
            for (Message message : messages) {
                if (message.getMessageType().equals(MessageType.ROBOT_BUILT)) {
                    System.out.println("Found an update");
                    if (((RobotBuiltMessage) message).getRobotType() == RobotType.LANDSCAPER) {
                        System.out.println("It was good");
                        roundLastUpdated = roundNum;
                        return ((RobotBuiltMessage) message).getRobotLocation();
                    }
                }
            }
        }


        return locationOfLandscaper;
    }

    public Goal getGoalFromMessage() throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(rc.getRoundNum()-1, goal);
        for (Message message : messages) {
            if (message.getMessageType().equals(MessageType.CHANGE_GOAL)) {
                return ((ChangeGoalMessage) message).getChangeGoalTo();
            }
        }

        return goal;
    }
}
