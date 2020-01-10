package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.RobotBuiltMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.Arrays;
import java.util.HashSet;

import static jers.Constants.directions;

public class Miner extends Robot {
    private final int MIN_SOUP_FOR_TRANSACTION = 2 * RobotType.MINER.soupLimit;

    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation designSchoolLocation = null;
    private MapLocation soupLocation = null;
    boolean designSchoolTransactionNeeded = false;
    private Goal goal = Goal.IDLE;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        pathFinder = new PathFinder(rc);
        transactor = new Transactor(rc);
        MapLocation soup = findSoup();

        if (soup != null) {
            if (rc.senseSoup(soup) > MIN_SOUP_FOR_TRANSACTION) {
                //Send a transaction to tell other miners to come here
            }

            pathFinder.setGoal(soup);
            goal = Goal.MINE;
        }
    }

    @Override
    public void run(int roundNum) throws GameActionException, IllegalStateException {
        if (designSchoolLocation == null) {
            // We're in the initial waiting phase; use this to check all previous turns for a design school.
            if (roundNum - createdOnRound < GameConstants.INITIAL_COOLDOWN_TURNS && (roundNum - createdOnRound - 1) * 99 < roundNum) {
                designSchoolLocation = checkRobotBuiltInRange(Math.max(1, (roundNum - createdOnRound - 1) * 99), Math.min(roundNum, (roundNum - createdOnRound) * 99), RobotType.DESIGN_SCHOOL);
            } else {
                designSchoolLocation = checkRobotBuiltInRound(roundNum - 1, RobotType.DESIGN_SCHOOL);
            }
        }

        if (designSchoolLocation == null && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
            MapLocation loc = makeBuilding(RobotType.DESIGN_SCHOOL);
            if (loc != null) {
                designSchoolLocation = loc;
                designSchoolTransactionNeeded = true;
            }
        }

        if (designSchoolTransactionNeeded) {
            designSchoolTransactionNeeded = !transactor.submitTransaction(new RobotBuiltMessage(new RobotType[]{RobotType.MINER, RobotType.HQ}, Goal.ALL, designSchoolLocation, RobotType.DESIGN_SCHOOL));
        }

        if (!rc.isReady()) {
            return;
        }

        switch (goal) {
            case IDLE:
                idle();
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

    private void idle() throws GameActionException {
        if (soupLocation == null) {
            soupLocation = findSoup();
        }

        if (soupLocation != null) {
            pathFinder.setGoal(soupLocation);
            goal = goal.MINE;
        } else {
            goal = Goal.EXPLORE;
        }
    }

    private void mine() throws GameActionException {
        boolean success = pathFinder.move(false);

        MapLocation goalLoc = pathFinder.getGoal();
        if (!success || (rc.canSenseLocation(goalLoc) && rc.senseSoup(goalLoc) == 0)) {
            goal = Goal.IDLE;
            soupLocation = null;
        }

        if (rc.senseSoup(rc.getLocation()) > 0 && rc.canMineSoup(Direction.CENTER)) {
            rc.mineSoup(Direction.CENTER);
        }

        if (rc.getSoupCarrying() >= RobotType.MINER.soupLimit) {
            goal = Goal.REFINE;
            pathFinder.setGoal(findOpenAdjacent(myHQ, rc.getLocation().directionTo(myHQ).opposite(), new HashSet<>(Arrays.asList(directions))));
        }
    }

    private void refine() throws GameActionException {
        boolean success = pathFinder.move(false);
        if (!success) {
            goal = Goal.IDLE;
        }

        Direction dirToRefinery = rc.getLocation().directionTo(myHQ);
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
            if (rc.canBuildRobot(type, d) && !rc.senseFlooding(buildAt) && rc.senseSoup(buildAt) == 0 && buildAt.distanceSquaredTo(myHQ) >= 9) {
                rc.buildRobot(type, d);
                return buildAt;
            }
        }

        return null;
    }

    /**
     * Finds the nearest location with soup that we can mine.
     * @return The nearest location with soup.
     * @throws GameActionException
     */
    private MapLocation findSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
        MapLocation argClosest = null;
        int minDist = Integer.MAX_VALUE;

        for (int x = loc.x-5; x <= loc.x+5; x++) {
            for (int y = loc.y - 5; y <= loc.y+5; y++) {
                MapLocation newLoc = new MapLocation(x, y);
                if (rc.canSenseLocation(newLoc) && rc.senseSoup(newLoc) > 0 && (!rc.isLocationOccupied(newLoc) || newLoc.equals(rc.getLocation())) && !rc.senseFlooding(newLoc)) {
                    int dist = rc.getLocation().distanceSquaredTo(newLoc);
                    if (dist < minDist) {
                        minDist = dist;
                        argClosest = newLoc;
                    }
                }
            }
        }

        return argClosest;
    }
}
