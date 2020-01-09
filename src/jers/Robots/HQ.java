package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.RobotBuiltMessage;

public class HQ extends Robot {
    boolean refineryBuilt = false;
    boolean buildMiner = true;
    boolean needToSendTransaction = false;
    MapLocation lastMinerLoc;
    boolean locationBroadcast = false;

    public HQ(RobotController rc) throws GameActionException {
        super(rc);
        lastMinerLoc = makeRobot(RobotType.MINER);
        goal = Goal.WAIT_FOR_REFINERY;
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (!locationBroadcast && transactor.submitTransaction(new RobotBuiltMessage((RobotType[]) Constants.robotTypes.toArray(), Goal.ALL, rc.getLocation(), RobotType.HQ))) {
            locationBroadcast = true;
        }

        switch (goal) {
            case WAIT_FOR_REFINERY:
                waitForRefinery(roundNum);
                break;
            case BUILD_LANDSCAPERS_AND_MINERS:
                buildLandscapersAndMiners(roundNum);
                break;
            default:
                throw new IllegalStateException("Invalid goal for HQ: " + goal);
        }
    }

    private void waitForRefinery(int roundNum) throws GameActionException {
        if (roundNum == 1) {
            return;
        }

        if (refineryBuilt || checkRobotBuiltInRound(roundNum - 1, RobotType.REFINERY) != null) {
            refineryBuilt = true;
            if ((lastMinerLoc = makeRobot(RobotType.MINER)) != null) {
                goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
            }
        }
    }

    private void buildLandscapersAndMiners(int roundNum) throws GameActionException {
        if (buildMiner && (lastMinerLoc = makeRobot(RobotType.MINER)) != null) {
            buildMiner = false;
            needToSendTransaction = true;
        } else if (!buildMiner && checkRobotBuiltInRound(roundNum - 1, RobotType.LANDSCAPER) != null) {
            buildMiner = true;
        }

        if (needToSendTransaction && transactor.submitTransaction(new RobotBuiltMessage(new RobotType[]{RobotType.DESIGN_SCHOOL}, Goal.BUILD_LANDSCAPERS_AND_MINERS, lastMinerLoc, RobotType.MINER))) {
            needToSendTransaction = false;
        }
    }
}
