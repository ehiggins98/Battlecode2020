package jers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
    private MapLocation current;

    public PathFinder(RobotController rc, MapLocation start, MapLocation goal) {
        this.rc = rc;
        this.goal = goal;
        this.current = start;
        visited = new HashSet<MapLocation>();
    }

    /**
     * Move to the next square on the path
     *
     * @return A value indicating whether we can move. This will only be false if the goal is
     * unreachable.
     */
    public boolean move() throws GameActionException {
        MapLocation argmin = null;
        Direction argminDir = null;
        double min = Double.POSITIVE_INFINITY;

        for (Direction d : RobotPlayer.directions) {
            MapLocation newLoc = this.current.add(d);
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
        current = argmin;
        rc.move(argminDir);
        return true;
    }

    public boolean isFinished() {
        return current.equals(goal);
    }
}
