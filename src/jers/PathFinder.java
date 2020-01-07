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

    public PathFinder(RobotController rc) {
        this.rc = rc;
        visited = new HashSet<MapLocation>();
    }

    public void setGoal(MapLocation goal) {
        this.goal = goal;
    }

    public MapLocation getGoal() { return this.goal; }

    /**
     * Move to the next square on the path
     *
     * @return A value indicating whether we can move. This will be false if we have reached the
     * goal or if the goal is unreachable.
     */
    public boolean move() throws GameActionException {
        if (this.rc.getLocation().equals(this.goal)) {
            return false;
        }
        MapLocation argmin = null;
        Direction argminDir = null;
        double min = Double.POSITIVE_INFINITY;

        for (Direction d : Constants.directions) {
            MapLocation newLoc = this.rc.getLocation().add(d);
            if (rc.canMove(d) && !visited.contains(newLoc)) {
                double dist;
                if ((dist = newLoc.distanceSquaredTo(goal)) < min) {
                    min = dist;
                    argmin = newLoc;
                    argminDir = d;
                }
            }
        }

        if (argmin == null) {
            return false;
        }

        visited.add(argmin);
        rc.move(argminDir);
        return true;
    }

    public boolean isFinished() {
        return this.rc.getLocation().equals(goal);
    }
}
