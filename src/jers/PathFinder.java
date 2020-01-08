package jers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
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
     *
     * @return A value indicating whether we can move. This will be false only if the goal is unreachable.
     */
    public boolean move() throws GameActionException {
        steps += 1;
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
            if (rc.canMove(d) && !visited.contains(newLoc) && !isFlooded(d)) {
                double dist;
                if ((dist = newLoc.distanceSquaredTo(goal)) < min) {
                    min = dist;
                    argmin = newLoc;
                    argminDir = d;
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

        visited.add(argmin);
        rc.move(argminDir);
        return true;
    }

    public boolean isFinished() {
        return this.rc.getLocation().equals(goal) || failed;
    }

    private boolean isFlooded(Direction d) throws GameActionException {
        return rc.senseFlooding(rc.adjacentLocation(d));
    }
}
