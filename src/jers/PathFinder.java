package jers;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * This currently implements the A* algorithm. This is really bad, but it's fine for now.
 */
public class PathFinder {
    private RobotController rc;
    private MapLocation goal;
    private PriorityQueue<PrioritizedMapLocation> queue;

    public PathFinder(RobotController rc, MapLocation start, MapLocation goal) {
        this.rc = rc;
        this.goal = goal;
        queue = new PriorityQueue<PrioritizedMapLocation>(16, new PrioritizedLocComparator());
        queue.add(new PrioritizedMapLocation(start, 0, heuristicDistance(start, goal)));
    }

    /**
     * Move to the next square on the path
     *
     * @return A value indicating whether we can move. This will only be false if the goal is
     * unreachable.
     */
    public boolean move() {
        if (queue.isEmpty()) {
            return false;
        }

        PrioritizedMapLocation current = queue.poll();
        for (Direction d : RobotPlayer.directions) {
            if (rc.canMove(d)) {
                MapLocation newLoc = current.getLocation().add(d)
                queue.add(new PrioritizedMapLocation(newLoc, current.getPrecedingDist() + 1, heuristicDistance(newLoc, this.goal)));
            }
        }
    }

    private double heuristicDistance(MapLocation current, MapLocation goal) {
        return Math.sqrt(current.distanceSquaredTo(goal);
    }

    class PrioritizedMapLocation {
        private MapLocation loc;
        private int precedingDist;
        private double heuristicDistRemaining;

        public PrioritizedMapLocation(MapLocation loc, int precedingDist, double heuristicDistRemaining) {
            this.loc = loc;
            this.precedingDist = precedingDist;
            this.heuristicDistRemaining = heuristicDistRemaining;
        }

        public MapLocation getLocation() {
            return this.loc;
        }

        public int getPrecedingDist() {
            return this.precedingDist;
        }

        public double getHeuristicDistRemaining() {
            return this.heuristicDistRemaining;
        }

        public double getPriority() {
            return this.precedingDist + this.heuristicDistRemaining;
        }
    }

    class PrioritizedLocComparator implements Comparator<PrioritizedMapLocation> {
        public int compare(PrioritizedMapLocation o1, PrioritizedMapLocation o2) {
            if (o1.getPriority() < o2.getPriority()) {
                return -1;
            } else if (o1.getPriority() == o2.getPriority()) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
