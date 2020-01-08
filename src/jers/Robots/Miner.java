package jers.Robots;

import battlecode.common.*;
import jers.Goal;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RefineryBuiltMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.ArrayList;
import java.util.Random;

import static jers.Constants.directions;

public class Miner extends Robot {
    private final int MIN_SOUP_FOR_TRANSACTION = 2 * RobotType.MINER.soupLimit;
    private final int MAX_EXPLORE_DELTA = 10;

    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation refineryLocation = null;
    private MapLocation soupLocation = null;
    boolean refineryTransactionNeeded = false;
    private Goal goal = Goal.IDLE;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        pathFinder = new PathFinder(rc);
        transactor = new Transactor(rc);
        MapLocation localSoup = findLocalSoup();
        if (localSoup != null) {
            if (rc.senseSoup(localSoup) > MIN_SOUP_FOR_TRANSACTION) {
                //Send a transaction to tell other miners to come here
            }

            pathFinder.setGoal(localSoup);
            goal = Goal.MINE;
        }
    }

    @Override
    public void run(int roundNum) throws GameActionException, IllegalStateException {
        if (refineryLocation == null && roundNum > 59) {
            refineryLocation = findRefineryLocation(roundNum);
        }
        if (refineryTransactionNeeded) {
            refineryTransactionNeeded = !transactor.submitTransaction(new RefineryBuiltMessage(new RobotType[]{RobotType.MINER}, Goal.ALL, refineryLocation));
            System.out.println("Transaction submitted? " + !refineryTransactionNeeded);
        }

        if (refineryLocation == null) {
            MapLocation loc = makeRefinery();
            if (loc != null) {
                refineryLocation = loc;
                refineryTransactionNeeded = true;
            }
        }

        if (!rc.isReady()) {
            return;
        }

        switch (goal) {
            case IDLE:
                findSoup();
                break;
            case MINE:
                mine();
                break;
            case REFINE:
                refine();
                break;
            case EXPLORE:
                explore();
                break;
            default:
                throw new IllegalStateException("Invalid goal " + goal);
        }
    }

    private void findSoup() throws GameActionException {
        MapLocation localSoup = findLocalSoup();
        MapLocation farSoup = findFarSoup();
        if (soupLocation != null) {
            pathFinder.setGoal(soupLocation);
            goal = goal.MINE;
        }
        else if (localSoup != null) {
            pathFinder.setGoal(localSoup);
            goal = Goal.MINE;
        }
        else if (farSoup != null) {
            pathFinder.setGoal(farSoup);
            goal = Goal.MINE;
        } else {
            goal = Goal.EXPLORE;
        }
    }

    private void mine() throws GameActionException {
        boolean success = pathFinder.move();

        MapLocation goalLoc = pathFinder.getGoal();
        if (!success || (rc.canSenseLocation(goalLoc) && rc.senseSoup(goalLoc) == 0)) {
            goal = Goal.IDLE;
        }

        if (rc.senseSoup(rc.getLocation()) > 0 && rc.canMineSoup(Direction.CENTER)) {
            rc.mineSoup(Direction.CENTER);
        }

        if (rc.getSoupCarrying() >= RobotType.MINER.soupLimit && refineryLocation != null) {
            goal = Goal.REFINE;
            pathFinder.setGoal(refineryLocation.subtract(rc.getLocation().directionTo(refineryLocation)));
        }
    }

    private void refine() throws GameActionException {
        boolean success = pathFinder.move();
        if (!success) {
            goal = Goal.IDLE;
        }

        Direction dirToRefinery = rc.getLocation().directionTo(refineryLocation);
        if (rc.canDepositSoup(dirToRefinery) && rc.isReady()) {
            rc.depositSoup(dirToRefinery, rc.getSoupCarrying());
            goal = Goal.IDLE;
        }
    }

    private void explore() throws GameActionException {
        boolean success = pathFinder.move();
        if (!success || pathFinder.isFinished()) {
            pathFinder.setGoal(getRandomGoal());
        }

        goal = Goal.IDLE;
    }

    private MapLocation getRandomGoal() {
        Random random = new Random();
        int dx = random.nextInt(MAX_EXPLORE_DELTA * 2 + 1) - MAX_EXPLORE_DELTA;
        int dy = random.nextInt(MAX_EXPLORE_DELTA * 2 + 1) - MAX_EXPLORE_DELTA;
        MapLocation currentLoc = rc.getLocation();
        return new MapLocation(currentLoc.x + dx, currentLoc.y + dy);
    }

    private MapLocation makeRefinery() throws GameActionException {
        for (Direction d : directions) {
            MapLocation buildAt = rc.getLocation().add(d);
            if (rc.canBuildRobot(RobotType.REFINERY, d) && !rc.senseFlooding(buildAt) && rc.senseSoup(buildAt) == 0) {
                rc.buildRobot(RobotType.REFINERY, d);
                return buildAt;
            }
        }

        return null;
    }

    private MapLocation findRefineryLocation(int roundNum) throws GameActionException {
        // Searching a round costs 100 bytecode, so we'll limit to 50 rounds to
        // be safe.
        for (int round = 69; round < Math.min(119, roundNum); round++) {
            ArrayList<Message> messages = transactor.getBlock(round, this.goal);
            for (Message m : messages) {
                if (m.getMessageType() == MessageType.REFINERY_BUILT) {
                    return ((RefineryBuiltMessage) m).getRefineryLocation();
                }
            }
        }

        return null;
    }

    /**
     * Find first instance of soup on the outskirts of the range
     * Use this after moving to determine if new soup has been discovered
     * If new soup is found, send transaction detailing location of soup
     * Locations are manually input so we don't have to check if within sensor range, just if on the map;
     * @return
     * @throws GameActionException
     */
    private MapLocation findFarSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int[][] coords = new int[][] {
                new int[]{loc.x-4, loc.y-4},
                new int[]{loc.x-3, loc.y-5},
                new int[]{loc.x-2, loc.y-5},
                new int[]{loc.x-1, loc.y-5},
                new int[]{loc.x, loc.y-5},
                new int[]{loc.x+1, loc.y-5},
                new int[]{loc.x+2, loc.y-5},
                new int[]{loc.x+3, loc.y-5},
                new int[]{loc.x+4, loc.y-4},
                new int[]{loc.x+5, loc.y-3},
                new int[]{loc.x+5, loc.y-2},
                new int[]{loc.x+5, loc.y-1},
                new int[]{loc.x+5, loc.y},
                new int[]{loc.x+5, loc.y+1},
                new int[]{loc.x+5, loc.y+2},
                new int[]{loc.x+5, loc.y+3},
                new int[]{loc.x+4, loc.y+4},
                new int[]{loc.x-3, loc.y+5},
                new int[]{loc.x-2, loc.y+5},
                new int[]{loc.x-1, loc.y+5},
                new int[]{loc.x, loc.y+5},
                new int[]{loc.x+1, loc.y+5},
                new int[]{loc.x+2, loc.y+5},
                new int[]{loc.x+3, loc.y+5},
                new int[]{loc.x-4, loc.y+4},
                new int[]{loc.x-5, loc.y-3},
                new int[]{loc.x-5, loc.y-2},
                new int[]{loc.x-5, loc.y-1},
                new int[]{loc.x-5, loc.y},
                new int[]{loc.x-5, loc.y+1},
                new int[]{loc.x-5, loc.y+2},
                new int[]{loc.x-5, loc.y+3}};

        for (int[] coord: coords) {
            MapLocation newLoc = new MapLocation(coord[0], coord[1]);
            try {
                if (rc.canSenseLocation(newLoc) && rc.onTheMap(newLoc) && rc.senseSoup(newLoc) > 0 && !rc.isLocationOccupied(newLoc) && !rc.senseFlooding(newLoc)) {
                    return newLoc;
                }
            }
            catch (GameActionException e) {
                //System.out.println("Not valid coords: " + coord[0] + " " + coord[1]);
            }
        }

        return null;
    }

    /**
     * Find first instance of soup in the range
     * For MapLocations in x-4, y-4 to x+4, y+4 check if soup is present
     * If so, send transaction detailing location of soup
     * @return
     * @throws GameActionException
     */
    private MapLocation findLocalSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
        for (int x = loc.x-4; x <= loc.x+4; x++) {
            for (int y = loc.y - 4; y <= loc.y+4; y++) {
                if (x == loc.x-4 && y == loc.y-4 || x == loc.x-4 && y == loc.y+4 || x == loc.x+4 && y == loc.y-4 || x == loc.x+4 && y == loc.y+4) {
                    continue;
                }
                MapLocation newLoc = new MapLocation(x, y);
                if (rc.canSenseLocation(newLoc) && rc.onTheMap(newLoc) && rc.senseSoup(newLoc) > 0 && !rc.isLocationOccupied(newLoc) && !rc.senseFlooding(newLoc)) {
                    return newLoc;
                }
            }
        }

        return null;
    }
}
