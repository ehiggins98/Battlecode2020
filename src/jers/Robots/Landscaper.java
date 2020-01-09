package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.PathFinder;

import java.util.ArrayList;

public class Landscaper extends Robot {

    // Map is rotationally, horizontally, or vertically symmetric, so we don't know for sure where the HQ is.
    private MapLocation[] theirHQ;
    private int hqTry;
    private PathFinder pathFinder;
    private MapLocation hqLoc;
    int deposited = 0;

    public Landscaper(RobotController rc) throws GameActionException {
        super(rc);
        MapLocation myHQ = checkRobotBuiltInRange(1, 50, RobotType.HQ);
        goal = Goal.FIND_ENEMY_HQ;
        theirHQ = calculateEnemyHQLocations(myHQ);
        hqTry = 0;
        pathFinder = new PathFinder(rc);
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        switch (goal) {
            case FIND_ENEMY_HQ:
                findEnemyHQ();
                break;
            case ATTACK_ENEMY_HQ:
                attackEnemyHQ();
                break;
            default:
                throw new IllegalStateException("Invalid goal for landscaper " + goal);
        }
    }

    private void findEnemyHQ() throws GameActionException {
        if (pathFinder.getGoal() == null || (pathFinder.isFinished() && !canSeeEnemyHQ())) {
            if (hqTry < 3) {
                pathFinder.setGoal(theirHQ[hqTry].subtract(rc.getLocation().directionTo(theirHQ[hqTry])));
                hqTry++;
            } else {
                // We can't get to the enemy HQ
                goal = Goal.IDLE;
            }
        } else if (pathFinder.isFinished() && canSeeEnemyHQ()) {
            if (rc.getLocation().isAdjacentTo(theirHQ[hqTry - 1])) {
                goal = Goal.ATTACK_ENEMY_HQ;
                hqLoc = theirHQ[hqTry-1];
            } else {
                MapLocation newGoal = getNewGoal();
                if (newGoal == null) {
                    goal = Goal.IDLE;
                } else {
                    pathFinder.setGoal(newGoal);
                }
            }
        }
        if (rc.isReady()) {
            pathFinder.move(true);
        }
    }

    private void attackEnemyHQ() throws GameActionException {
        Direction hqDir = rc.getLocation().directionTo(hqLoc);
        if (rc.canDepositDirt(hqDir)) {
            rc.depositDirt(hqDir);
        } else {
            digDirt();
        }
    }

    private void digDirt() throws GameActionException {
        for (Direction d : Constants.directions) {
            RobotInfo info = rc.senseRobotAtLocation(rc.getLocation().add(d));
            if (rc.canDigDirt(d) && (info == null || info.getType() != RobotType.HQ)) {
                rc.digDirt(d);
                return;
            }
        }
    }

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

    private MapLocation getNewGoal() throws GameActionException {
        for (Direction d : Constants.directions) {
            MapLocation newGoal = theirHQ[hqTry - 1].add(d);
            if (!rc.isLocationOccupied(newGoal)) {
                return newGoal;
            }
        }

        return null;
    }

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
}
