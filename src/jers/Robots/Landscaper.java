package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.PathFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Landscaper extends Robot {
    // Map is rotationally, horizontally, or vertically symmetric, so we don't know for sure where the HQ is.
    private MapLocation[] theirHQPossibilities;
    private int hqTry;
    private MapLocation theirHQ;
    private PathFinder pathFinder;
    private Direction depositDirection = Direction.CENTER;

    public Landscaper(RobotController rc) throws GameActionException {
        super(rc);
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        theirHQPossibilities = calculateEnemyHQLocations(myHQ);
        hqTry = 0;
        pathFinder = new PathFinder(rc);
    }

    /**
     * Some landscapers build a wall around the HQ, while others find the enemy HQ and attempt to bury it.
     * These attacking landscapers will dig through walls built by the enemy.
     * @param roundNum The current round number.
     * @throws GameActionException
     */
    @Override
    public void run(int roundNum) throws GameActionException {
        if ((goal == null || goal == Goal.IDLE) && roundNum - createdOnRound < GameConstants.INITIAL_COOLDOWN_TURNS) {
            goal = checkInitialGoal(rc.getLocation(), roundNum);
        }

        Goal lastGoal = null;

        // Without the while loop we waste turns changing goals
        while (rc.isReady() && goal != null && goal != Goal.IDLE && lastGoal != goal) {
            lastGoal = goal;
            switch (goal) {
                case IDLE:
                    break;
                case FIND_ENEMY_HQ:
                    findEnemyHQ();
                    break;
                case ATTACK_ENEMY_HQ:
                    attackEnemyHQ();
                    break;
                case GO_TO_MY_HQ:
                    goToMyHQ();
                    break;
                case BUILD_HQ_WALL:
                    buildHQWall();
                    break;
                default:
                    throw new IllegalStateException("Invalid goal for landscaper " + goal);
            }
        }
    }

    // Find the enemy HQ. We know the map is horizontally, vertically, or rotationally symmetric, so we have only
    // three locations where it can be. Thus, we try these possibilities until we find the HQ, then switch to
    // the attack goal.
    private void findEnemyHQ() throws GameActionException {
        if (pathFinder.getGoal() == null || (pathFinder.isFinished() && !canSeeEnemyHQ())) {
            if (hqTry < 3) {
                pathFinder.setGoal(theirHQPossibilities[hqTry].subtract(rc.getLocation().directionTo(theirHQPossibilities[hqTry])));
                hqTry++;
            } else {
                // We can't get to the enemy HQ
                goal = Goal.IDLE;
            }
        } else if (pathFinder.isFinished() && canSeeEnemyHQ()) {
            if (rc.getLocation().isAdjacentTo(theirHQPossibilities[hqTry - 1])) {
                goal = Goal.ATTACK_ENEMY_HQ;
                theirHQ = theirHQPossibilities[hqTry-1];
            } else {
                MapLocation newGoal = getOpenTileAdjacent(theirHQPossibilities[hqTry-1],
                        theirHQPossibilities[hqTry-1].directionTo(rc.getLocation()), Constants.directions, false);

                if (newGoal == null) {
                    goal = Goal.IDLE;
                } else {
                    pathFinder.setGoal(newGoal);
                }
            }
        }
        if (rc.isReady()) {
            pathFinder.move(true, false);
        }
    }

    // Try to bury the enemy HQ.
    private void attackEnemyHQ() throws GameActionException {
        /*Direction hqDir = rc.getLocation().directionTo(theirHQ);
        if (rc.canDepositDirt(hqDir)) {
            rc.depositDirt(hqDir);
        } else if (rc.canDigDirt(Direction.CENTER)) {
            rc.digDirt(Direction.CENTER);
        }*/
    }

    // For defensive landscapers, go to our HQ. Since we have 4 landscapers building the wall, they stand at the
    // cardinal directions and build 2 tiles of the wall.
    private void goToMyHQ() throws GameActionException { ;
        if (pathFinder.getGoal() == null || pathFinder.isFailed()) {
            pathFinder.setGoal(getOpenTileAdjacent(myHQ, myHQ.directionTo(rc.getLocation()), Constants.cardinalDirections, false));
        } else if (pathFinder.isFinished()) {
            goal = Goal.BUILD_HQ_WALL;
        }

        if (rc.isReady()) {
            pathFinder.move(false, false);
        }
    }

    // Build the wall. Each landscaper handles 2 tiles.
    private void buildHQWall() throws GameActionException {
        RobotInfo occupier = rc.senseRobotAtLocation(rc.getLocation().add(depositDirection));
        if (rc.canDepositDirt(depositDirection) && rc.getDirtCarrying() > 0 &&
                (occupier == null || occupier.getType() != RobotType.LANDSCAPER || depositDirection == Direction.CENTER)) {
            rc.depositDirt(depositDirection);

            if (depositDirection == Direction.CENTER) {
                depositDirection = rc.getLocation().directionTo(myHQ).rotateLeft().rotateLeft();
            } else {
                depositDirection = Direction.CENTER;
            }
            return;
        }

        Direction digDirection = getDigDirection();
        if (digDirection != null && rc.canDigDirt(digDirection)) {
            rc.digDirt(digDirection);
        }
    }

    // Check if we can see the enemy HQ.
    private boolean canSeeEnemyHQ() throws GameActionException {
        int radius = (int)Math.sqrt(rc.getType().sensorRadiusSquared);
        for (int dx = -radius; dx < radius; dx++) {
            for (int dy = -radius; dy < radius; dy++) {
                MapLocation toCheck = rc.getLocation().translate(dx, dy);
                if (!rc.canSenseLocation(toCheck)) {
                    continue;
                }

                RobotInfo info = rc.senseRobotAtLocation(toCheck);
                if (info != null && info.getType() == RobotType.HQ && info.getTeam() != rc.getTeam()) {
                    return true;
                }
            }
        }

        return false;
    }

    // Calculate possible locations of the enemy HQ, given that the map is horizontally, vertically, or rotationally
    // symmetric.
    private MapLocation[] calculateEnemyHQLocations(MapLocation myHQ) {
        ArrayList<MapLocation> locations = new ArrayList<MapLocation>(3);
        // Vertical axis of symmetry
        locations.add(new MapLocation(rc.getMapWidth() - myHQ.x - 1, myHQ.y));
        // Horizontal axis of symmetry
        locations.add(new MapLocation(myHQ.x, rc.getMapHeight() - myHQ.y - 1));
        // Rotationally symmetric
        locations.add(new MapLocation(rc.getMapWidth() - myHQ.x - 1, rc.getMapHeight() - myHQ.y - 1));

        // Sort them so that we take the most efficient path between the 3.
        MapLocation[] sorted = new MapLocation[3];
        int added = 0;

        while (added < 3) {
            int minDist = Integer.MAX_VALUE;
            int argmin = 0;
            for (int i = 0; i < locations.size(); i++) {
                int dist;
                if (added == 0) {
                    dist = rc.getLocation().distanceSquaredTo(locations.get(i));
                } else {
                    dist = sorted[added-1].distanceSquaredTo(locations.get(i));
                }

                if (dist < minDist) {
                    minDist = dist;
                    argmin = i;
                }
            }

            sorted[added] = locations.get(argmin);
            locations.remove(argmin);
            added++;
        }

        return sorted;
    }

    // Find the direction in which to dig dirt while building the HQ wall. This will avoid destroying the wall
    // and won't dig other landscapers into a ditch.
    private Direction getDigDirection() throws GameActionException {
        Direction perpendicular = myHQ.directionTo(rc.getLocation());
        Direction[] possibilities = new Direction[]{perpendicular, perpendicular.rotateLeft(), perpendicular.rotateLeft()};

        for (Direction d : possibilities) {
            RobotInfo occupier = rc.senseRobotAtLocation(rc.getLocation().add(d));
            if (rc.canDigDirt(d) && (occupier == null || occupier.getType() != RobotType.LANDSCAPER)) {
                return d;
            }
        }

        return null;
    }
}
