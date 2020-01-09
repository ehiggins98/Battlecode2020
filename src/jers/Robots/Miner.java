package jers.Robots;

import battlecode.common.*;
import jers.Goal;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RobotBuiltMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.ArrayList;
import java.util.Random;

import static jers.Constants.directions;

public class Miner extends Robot {
    private final int MIN_SOUP_FOR_TRANSACTION = 2 * RobotType.MINER.soupLimit;

    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation refineryLocation = null;
    private MapLocation designSchoolLocation = null;
    private MapLocation soupLocation = null;
    private MapLocation hqLocation = null;
    boolean refineryTransactionNeeded = false;
    boolean designSchoolTransactionNeeded = false;
    private Goal goal = Goal.IDLE;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        pathFinder = new PathFinder(rc);
        transactor = new Transactor(rc);
        MapLocation localSoup = findLocalSoup();
        hqLocation = checkRobotBuiltInRange(1, 50, RobotType.HQ);
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
            refineryLocation = checkRobotBuiltInRange(59, Math.min(roundNum, 119), RobotType.REFINERY);
        }
        if (designSchoolLocation == null && roundNum > 100) {
            designSchoolLocation = checkRobotBuiltInRange(100, Math.min(roundNum, 150), RobotType.DESIGN_SCHOOL);
        }

        if (refineryTransactionNeeded) {
            refineryTransactionNeeded = !transactor.submitTransaction(new RobotBuiltMessage(new RobotType[]{RobotType.MINER, RobotType.HQ}, Goal.ALL, refineryLocation, RobotType.REFINERY));
        }

        if (designSchoolTransactionNeeded) {
            designSchoolTransactionNeeded = !transactor.submitTransaction(new RobotBuiltMessage(new RobotType[]{RobotType.MINER, RobotType.HQ}, Goal.ALL, designSchoolLocation, RobotType.DESIGN_SCHOOL));
        }

        if (refineryLocation == null && rc.getTeamSoup() >= RobotType.REFINERY.cost) {
            MapLocation loc = makeBuilding(RobotType.REFINERY);
            if (loc != null) {
                refineryLocation = loc;
                refineryTransactionNeeded = true;
            }
        }

        if (refineryLocation != null && designSchoolLocation == null && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
            MapLocation loc = makeBuilding(RobotType.DESIGN_SCHOOL);
            if (loc != null) {
                designSchoolLocation = loc;
                designSchoolTransactionNeeded = true;
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
                throw new IllegalStateException("Invalid goal for miner " + goal);
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
        boolean success = pathFinder.move(false);

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
        boolean success = pathFinder.move(false);
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
        boolean success = pathFinder.move(false);
        if (!success || pathFinder.isFinished()) {
            pathFinder.setGoal(getRandomGoal());
        }

        goal = Goal.IDLE;
    }

    private MapLocation makeBuilding(RobotType type) throws GameActionException {
        for (Direction d : directions) {
            MapLocation buildAt = rc.getLocation().add(d);
            if (rc.canBuildRobot(type, d) && !rc.senseFlooding(buildAt) && rc.senseSoup(buildAt) == 0 && !buildAt.isAdjacentTo(hqLocation)) {
                rc.buildRobot(type, d);
                return buildAt;
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
                System.out.println("Not valid coords: " + coord[0] + " " + coord[1]);
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
