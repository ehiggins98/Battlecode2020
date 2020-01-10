package jers.Robots;

import battlecode.common.*;
import jers.Goal;
import jers.Messages.RobotBuiltMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class Miner extends Robot {
    private final int MAX_EXPLORE_DELTA = 10;

    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation designSchoolLocation = null;
    private MapLocation soupLocation = null;
    boolean designSchoolTransactionNeeded = false;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        pathFinder = new PathFinder(rc);
        transactor = new Transactor(rc);
        MapLocation soup = findSoup();

        if (soup != null) {
            pathFinder.setGoal(soup);
            goal = Goal.MINE;
        } else {
            goal = Goal.EXPLORE;
        }
    }

    /**
     * Basically just mine and refine soup until the end of the game. If the miner can't see any soup, it starts
     * a semi-random walk around the map until it finds some, and then it mines there.
     * @param roundNum The current round.
     * @throws GameActionException
     * @throws IllegalStateException
     */
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

    // Execute the idle strategy - if we know where soup is, go there, otherwise look for soup in the vicinity, and
    // otherwise explore.
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

    // Execute the mine strategy - go to soup, mine it until we're full, then refine.
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
            pathFinder.setGoal(getOpenTileAdjacent(myHQ, rc.getLocation().directionTo(myHQ).opposite(), new HashSet<>(Arrays.asList(Direction.allDirections()))));
        }
    }

    // Go to the HQ and dump all our soup in, then go back to mining.
    private void refine() throws GameActionException {
        boolean success = pathFinder.move(false);
        if (!success) {
            goal = Goal.IDLE;
        }

        Direction dirToRefinery = rc.getLocation().directionTo(myHQ);
        if (rc.canDepositSoup(dirToRefinery)) {
            rc.depositSoup(dirToRefinery, rc.getSoupCarrying());
            goal = Goal.IDLE;
        }
    }

    // Walk semi-randomly around the map until we see soup. This just generates random goals at most MAX_EXPLORE_DELTA
    // tiles away from our current location and goes there. If there's no soup visible, it generates another goal.
    private void explore() throws GameActionException {
        boolean success = pathFinder.move(false);
        if (!success || pathFinder.isFinished()) {
            pathFinder.setGoal(getRandomGoal());
        }

        goal = Goal.IDLE;
    }

    // Make a building. This ensures we don't put the building in water, that it's not on top of soup, and that it's far enough away
    // from the HQ (just so the building doesn't interfere with building a wall around the HQ.
    private MapLocation makeBuilding(RobotType type) throws GameActionException {
        for (Direction d : Direction.allDirections()) {
            MapLocation buildAt = rc.getLocation().add(d);
            if (rc.canBuildRobot(type, d) && !rc.senseFlooding(buildAt) && rc.senseSoup(buildAt) == 0 && buildAt.distanceSquaredTo(myHQ) >= 9) {
                rc.buildRobot(type, d);
                return buildAt;
            }
        }

        return null;
    }

    // Find the nearest visible soup.
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

    // Get a random goal, used for explore mode.
    MapLocation getRandomGoal() {
        Random random = new Random();
        int dx = random.nextInt(MAX_EXPLORE_DELTA * 2 + 1) - MAX_EXPLORE_DELTA;
        int dy = random.nextInt(MAX_EXPLORE_DELTA * 2 + 1) - MAX_EXPLORE_DELTA;
        MapLocation currentLoc = rc.getLocation();
        return new MapLocation(currentLoc.x + dx, currentLoc.y + dy);
    }
}
