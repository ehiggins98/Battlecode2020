package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import jers.Goal;
import jers.Messages.InitialGoalMessage;
import jers.Messages.RobotBuiltMessage;

import static jers.Constants.*;

public class FulfillmentCenter extends Robot {

    private int dronesBuilt;
    private boolean buildDrone;
    private InitialGoalMessage initialGoalMessage;
    private RobotBuiltMessage robotBuiltMessage;

    public FulfillmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        dronesBuilt = 0;
        buildDrone = true;
        goal = Goal.BUILD_INITIAL_DRONES;
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
    }


    @Override
    public void run(int roundNum) throws GameActionException {
        switch (goal) {
            case IDLE:
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

        if (dronesBuilt > INITIAL_ATTACKING_DRONES + INITIAL_DEFENDING_DRONES) {
            goal = Goal.IDLE;
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
        MapLocation locationOfPreviousLandscaper = checkRobotBuiltInRound(roundNum - 1, RobotType.LANDSCAPER);
        if (buildDrone && dronesBuilt < INITIAL_ATTACKING_DRONES + INITIAL_DEFENDING_DRONES + DRONE_LANDSCAPER_PAIRS) {
            MapLocation builtAt = makeRobot(RobotType.DELIVERY_DRONE);
            if (builtAt == null) {
                return;
            }

            buildDrone = false;
            dronesBuilt += 1;
            /*initialGoalMessage = new InitialGoalMessage(new RobotType[]{RobotType.DELIVERY_DRONE},
                    Goal.ALL, builtAt, roundNum, Goal.ATTACK_ENEMY_HQ);*/
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.HQ, RobotType.MINER},
                    Goal.ALL, builtAt, RobotType.DELIVERY_DRONE);
        } else if (!buildDrone && checkRobotBuiltInRound(roundNum - 1, RobotType.LANDSCAPER) != null) {
            buildDrone = true;
        }

        if (initialGoalMessage != null && transactor.submitTransaction(initialGoalMessage)) {
            initialGoalMessage = null;
        }

        if (robotBuiltMessage != null && transactor.submitTransaction(robotBuiltMessage)) {
            robotBuiltMessage = null;
        }
    }
}
