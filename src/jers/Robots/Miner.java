package jers.Robots;

import battlecode.common.*;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RefineryBuiltMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.ArrayList;

import static jers.Constants.directions;

public class Miner extends Robot {
    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation refineryLocation = null;
    boolean refineryTransactionNeeded = false;

    public Miner(RobotController rc) {
        super(rc);

        pathFinder = new PathFinder(rc);
        pathFinder.setGoal(new MapLocation(0, 0));
        transactor = new Transactor(rc);
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (refineryLocation == null && roundNum > 70) {
            refineryLocation = findRefineryLocation(roundNum);
        }
        if (refineryTransactionNeeded) {
            refineryTransactionNeeded = !transactor.submitTransaction(new RefineryBuiltMessage(refineryLocation));
        }

        if (refineryLocation == null) {
            MapLocation loc = makeRefinery();
            if (loc != null) {
                refineryLocation = loc;
                refineryTransactionNeeded = true;
            }
        }
        if (!pathFinder.isFinished()) {
            pathFinder.move();
        }
    }

    private MapLocation makeRefinery() throws GameActionException {
        for (Direction d : directions) {
            if (rc.canBuildRobot(RobotType.REFINERY, d)) {
                MapLocation builtAt = rc.getLocation().add(d);
                rc.buildRobot(RobotType.REFINERY, d);
                return builtAt;
            }
        }

        return null;
    }

    private MapLocation findRefineryLocation(int roundNum) throws GameActionException {
        // Searching a round costs 100 bytecode, so we'll limit to 50 rounds to
        // be safe.
        for (int round = 59; round < Math.min(109, roundNum); round++) {
            ArrayList<Message> messages = transactor.getBlock(round);
            for (Message m : messages) {
                if (m.getMessageType() == MessageType.REFINERY_BUILT) {
                    return ((RefineryBuiltMessage) m).getRefineryLocation();
                }
            }
        }

        return null;
    }
}
