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
    private MapLocation initialLocation;
    private boolean gotInitialGoal;
    private Direction depositDirection = Direction.CENTER;

    public Landscaper(RobotController rc) throws GameActionException {
        super(rc);
        goal = Goal.IDLE;
        theirHQ = calculateEnemyHQLocations(myHQ);
        hqTry = 0;
        pathFinder = new PathFinder(rc);
        initialLocation = rc.getLocation();
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (!gotInitialGoal && roundNum - createdOnRound < 10) {
            Goal initialGoal = checkInitialGoal(initialLocation, roundNum);
            if (initialGoal != null) {
                goal = initialGoal;
                gotInitialGoal = true;
            }
        }

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
        } else if (rc.canDigDirt(Direction.CENTER)) {
            rc.digDirt(Direction.CENTER);
        }
    }

    private void goToMyHQ() throws GameActionException {
        if (pathFinder.getGoal() == null || pathFinder.isFailed()) {
            for (Direction d : Direction.cardinalDirections()) {
                MapLocation newLoc = myHQ.add(d);
                if (rc.getLocation().equals(newLoc)) {
                    goal = Goal.BUILD_HQ_WALL;
                    return;
                }
            }

            for (Direction d : Direction.cardinalDirections()) {
                MapLocation newLoc = myHQ.add(d);
                if (rc.canSenseLocation(newLoc) && !rc.isLocationOccupied(newLoc) || !rc.canSenseLocation(newLoc)) {
                    pathFinder.setGoal(newLoc);
                    break;
                }
            }
        } else if (pathFinder.isFinished()) {
            goal = Goal.BUILD_HQ_WALL;
        }

        if (rc.isReady()) {
            pathFinder.move(false);
        }
    }

    private void buildHQWall() throws GameActionException {
        if (rc.canDepositDirt(depositDirection) && rc.getDirtCarrying() > 0) {
            rc.depositDirt(depositDirection);

            if (depositDirection == Direction.CENTER) {
                depositDirection = rc.getLocation().directionTo(myHQ).rotateLeft().rotateLeft();
            } else {
                depositDirection = Direction.CENTER;
            }
            return;
        }

        Direction[] diggingDirs = new Direction[]{
                rc.getLocation().directionTo(myHQ).opposite(),
                rc.getLocation().directionTo(myHQ).opposite().rotateLeft(),
                rc.getLocation().directionTo(myHQ).rotateRight()
        };

        for (Direction d : diggingDirs) {
            if (rc.canDigDirt(d)) {
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
