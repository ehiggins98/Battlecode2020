package jers;

import battlecode.common.*;

import java.util.HashSet;

/**
 * This is a really basic and bad algorithm and it really needs improvement.
 * It greedily selects the next unvisited square that's closest to the goal, and moves there.
 */
public class PathFinder {
    // Take at most 15 steps without finding a new closest point.
    private final int MAX_STEPS_WTIHOUT_CLOSEST_DIST_DECREASE = 15;

    private RobotController rc;
    private MapLocation goal;
    private HashSet<MapLocation> visited;
    private boolean failed;
    private int steps = 0;
    private double closestDist;
    private int closestDistUpdated;

    /**
     * Initializes the pathfinder with no goal.
     * @param rc The RobotController to use.
     */
    public PathFinder(RobotController rc) {
        this.rc = rc;
        failed = false;
        visited = new HashSet<>();
        closestDist = Integer.MAX_VALUE;
        closestDistUpdated = 0;
    }

    /**
     * Sets a new goal for the pathfinder. This resets all appropriate state,
     * and sets the pathfinder to a non-failed state.
     * @param goal The new goal to use.
     */
    public void setGoal(MapLocation goal) {
        visited.clear();
        failed = false;
        steps = 0;
        this.goal = goal;
        closestDist = Integer.MAX_VALUE;
        closestDistUpdated = 0;
    }

    /**
     * Get the current goal of the pathfinder.
     * @return The current goal of the pathfinder.
     */
    public MapLocation getGoal() { return this.goal; }

    /**
     * Gets a value indicating whether the pathfinder reached its goal successfully.
     * @return A value indicating whether the pathfinder reached its goal successfully.
     */
    public boolean isSuccess() {
        return this.rc.getLocation().equals(goal);
    }

    /**
     * Gets a value indicating whether the pathfinder failed in reaching its goal.
     * @return Gets a value indicating whether the pathfinder failed in reaching its goal.
     */
    public boolean isFailed() {
        return this.failed;
    }

    /**
     * Gets a value indicating whether the pathfinder is finished (i.e. failed or succeeded).
     * @return A value indicating whether the pathfinder is finished.
     */
    public boolean isFinished() {
        return isFailed() || isSuccess();
    }

    /**
     * Move to the next square on the path
     * @param tryDig Whether the pathfinder should try to dig if it hits a wall.
     * @return A value indicating whether we can move. This will be false only if the goal is unreachable.
     */
    public boolean move(boolean tryDig, boolean tryFly, MapLocation myHQ) throws GameActionException {
        if (tryDig && rc.getType() != RobotType.LANDSCAPER) {
            throw new IllegalArgumentException("Can't try to dig if unit is not landscaper");
        }
        if (tryFly && rc.getType() != RobotType.DELIVERY_DRONE) {
            throw new IllegalArgumentException("Can't try to fly if unit is not drone");
        }

        boolean dig = false;
        if (this.goal == null || steps - closestDistUpdated > MAX_STEPS_WTIHOUT_CLOSEST_DIST_DECREASE) {
            failed = true;
            return false;
        } else if (this.rc.getLocation().equals(this.goal)) {
            return true;
        }

        Direction nextStep = null;
        double min = Double.POSITIVE_INFINITY;

        for (Direction d : Direction.allDirections()) {
            MapLocation newLoc = rc.getLocation().add(d);
            boolean diggingMightHelp = tryDig && isWall(newLoc);

            if ((rc.canMove(d) || ((tryDig || tryFly) && isWall(newLoc))) && !visited.contains(newLoc) && (tryFly || !rc.senseFlooding(rc.getLocation().add(d)))) {
                if (tryDig && isWall(newLoc) && myHQ.distanceSquaredTo(rc.getLocation()) < Constants.FAR_THRESHOLD_RADIUS_SQUARED) {
                    continue;
                }

                double dist;
                if ((dist = newLoc.distanceSquaredTo(goal)) < min) {
                    min = dist;
                    nextStep = d;
                    dig = diggingMightHelp;
                }
            } else if (newLoc.equals(goal)) {
                failed = true;
                return false;
            }
        }

        if (nextStep == null) {
            failed = true;
            return false;
        }

        if (dig) {
            if (rc.getDirtCarrying() >= rc.getType().dirtLimit && !depositDirtAvoiding(nextStep)) {
                return false;
            }

            if (rc.senseElevation(rc.getLocation().add(nextStep)) > rc.senseElevation(rc.getLocation()) && rc.canDigDirt(nextStep)) {
                rc.digDirt(nextStep);
            } else if (rc.canDigDirt(Direction.CENTER)) {
                rc.digDirt(Direction.CENTER);
            }
        } else {
            if (min < closestDist) {
                closestDist = min;
                closestDistUpdated = steps;
            }

            visited.add(rc.getLocation().add(nextStep));
            rc.move(nextStep);
            rc.setIndicatorLine(rc.getLocation(), goal, 0, 255, 0);
            steps += 1;
        }

        return true;
    }

    // Get a value indicating whether digging would allow us to pass a barrier
    private boolean isWall(MapLocation newLoc) throws GameActionException {
        return rc.isReady() && rc.canSenseLocation(newLoc) && !rc.isLocationOccupied(newLoc) && Math.abs(rc.senseElevation(newLoc) - rc.senseElevation(rc.getLocation())) > GameConstants.MAX_DIRT_DIFFERENCE;
    }

    // Deposit dirt while avoiding the direction we came from and its opposite if possible.
    private boolean depositDirtAvoiding(Direction comingFrom) throws GameActionException {
        Direction[] precedence = Direction.allDirections();
        int moved = 0;
        // Don't deposit dirt in the direction we came from or are probably going,
        // because somebody else will probably come that way. Thus, we try those last.
        for (int i = 0; i < precedence.length; i++) {
            if (precedence[i] == comingFrom || precedence[i] == comingFrom.opposite()) {
                Direction temp = precedence[i];
                precedence[i] = precedence[precedence.length - moved - 1];
                precedence[precedence.length - moved - 1] = temp;
                moved++;
            }
        }

        for (Direction d : precedence) {
            if (rc.canDepositDirt(d)) {
                rc.depositDirt(d);
                return true;
            }
        }

        return false;
    }
}
