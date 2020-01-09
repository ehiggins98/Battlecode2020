package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.RobotBuiltMessage;

public class HQ extends Robot {
    private final int INITIAL_MINER_COUNT = 5;
    private boolean refineryBuilt = false;
    private boolean buildMiner = true;
    private boolean needToSendTransaction = false;
    private MapLocation lastMinerLoc;
    private boolean locationBroadcast = false;
    private int minersBuilt;

    public HQ(RobotController rc) throws GameActionException {
        super(rc);
        minersBuilt = 0;
        goal = Goal.BUILD_INITIAL_MINERS;
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (!locationBroadcast && transactor.submitTransaction(new RobotBuiltMessage((RobotType[]) Constants.robotTypes.toArray(), Goal.ALL, rc.getLocation(), RobotType.HQ))) {
            locationBroadcast = true;
        }

        switch (goal) {
            case BUILD_INITIAL_MINERS:
                buildInitialMiners(roundNum);
                break;
            case BUILD_LANDSCAPERS_AND_MINERS:
                buildLandscapersAndMiners(roundNum);
                break;
            default:
                throw new IllegalStateException("Invalid goal for HQ: " + goal);
        }
    }

    private void buildInitialMiners(int roundNum) throws GameActionException {
        if (rc.isReady() && rc.getTeamSoup() > RobotType.MINER.cost) {
            makeRobot(RobotType.MINER);
            minersBuilt += 1;
        }

        if (minersBuilt >= INITIAL_MINER_COUNT) {
            goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
        }
    }

    private void buildLandscapersAndMiners(int roundNum) throws GameActionException {
        if (buildMiner && (lastMinerLoc = makeRobot(RobotType.MINER)) != null) {
            buildMiner = false;
            needToSendTransaction = true;
            minersBuilt += 1;
        } else if (!buildMiner && checkRobotBuiltInRound(roundNum - 1, RobotType.LANDSCAPER) != null) {
            buildMiner = true;
        }

        if (needToSendTransaction && transactor.submitTransaction(new RobotBuiltMessage(new RobotType[]{RobotType.DESIGN_SCHOOL}, Goal.BUILD_LANDSCAPERS_AND_MINERS, lastMinerLoc, RobotType.MINER))) {
            needToSendTransaction = false;
        }
    }
}
