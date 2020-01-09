package jers;

import battlecode.common.*;
import com.sun.tools.internal.jxc.ap.Const;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This is a really basic and bad algorithm and it really needs improvement.
 * It greedily selects the next unvisited square that's closest to the goal, and moves there.
 */
public class PathFinder {
    private RobotController rc;
    private MapLocation goal;
    private HashSet<MapLocation> visited;
    private boolean failed;
    private final int MAX_STEPS_PER_GOAL = 90;
    private int steps = 0;

    public PathFinder(RobotController rc) {
        this.rc = rc;
        failed = false;
        visited = new HashSet<MapLocation>();
    }

    public void setGoal(MapLocation goal) {
        visited.clear();
        failed = false;
        steps = 0;
        this.goal = goal;
    }

    public MapLocation getGoal() { return this.goal; }

    /**
     * Move to the next square on the path
     * @param tryDig Whether the pathfinder should try to dig if it hits a wall.
     * @return A value indicating whether we can move. This will be false only if the goal is unreachable.
     */
    public boolean move(boolean tryDig) throws GameActionException {
        if (tryDig && rc.getType() != RobotType.LANDSCAPER) {
            throw new IllegalArgumentException("Can't try to dig if unit is not landscaper");
        }

        boolean dig = false;
        if (this.goal == null || steps > MAX_STEPS_PER_GOAL) {
            failed = true;
            return false;
        } else if (this.rc.getLocation().equals(this.goal)) {
            return true;
        }

        MapLocation argmin = null;
        Direction argminDir = null;
        double min = Double.POSITIVE_INFINITY;

        for (Direction d : Constants.directions) {
            MapLocation newLoc = this.rc.getLocation().add(d);
            boolean diggingMightHelp = tryDig && diggingWouldFixBarrier(newLoc) && tryDig;
            if ((rc.canMove(d) || diggingMightHelp) && !visited.contains(newLoc) && !isFlooded(d)) {
                double dist;
                if ((dist = newLoc.distanceSquaredTo(goal)) < min) {
                    min = dist;
                    argmin = newLoc;
                    argminDir = d;
                    dig = diggingMightHelp;
                }
            } else if (newLoc.equals(goal)) {
                failed = true;
                return false;
            }
        }

        if (argmin == null) {
            failed = true;
            return false;
        }

        if (dig) {
            if (rc.getDirtCarrying() >= rc.getType().dirtLimit && !depositDirt(argminDir)) {
                return false;
            }
            if (rc.senseElevation(argmin) > rc.senseElevation(rc.getLocation()) && rc.canDigDirt(argminDir)) {
                rc.digDirt(argminDir);
            } else if (rc.senseElevation(argmin) < rc.senseElevation(rc.getLocation()) && rc.canDigDirt(Direction.CENTER)) {
                rc.digDirt(Direction.CENTER);
            }
        } else {
            visited.add(argmin);
            rc.move(argminDir);
            steps += 1;
        }

        return true;
    }

    public boolean diggingWouldFixBarrier(MapLocation newLoc) throws GameActionException {
        return rc.isReady() && rc.canSenseLocation(newLoc) && !rc.isLocationOccupied(newLoc) && Math.abs(rc.senseElevation(newLoc) - rc.senseElevation(rc.getLocation())) > 3;
    }

    public boolean isFinished() {
        return this.rc.getLocation().equals(goal) || failed;
    }

    public boolean isFailed() {
        return this.failed;
    }

    private boolean isFlooded(Direction d) throws GameActionException {
        return rc.senseFlooding(rc.adjacentLocation(d));
    }

    private boolean depositDirt(Direction comingFrom) throws GameActionException {
        Direction[] precedence = Constants.directions;
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
