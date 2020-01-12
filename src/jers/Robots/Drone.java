package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.PathFinder;

import java.util.ArrayList;
import java.util.Map;

public class Drone extends Robot {
    // Map is rotationally, horizontally, or vertically symmetric, so we don't know for sure where the HQ is.
    private MapLocation[] theirHQPossibilities;
    private int hqTry;
    private MapLocation theirHQ;
    private PathFinder pathFinder;
    private int target_id;

    public Drone(RobotController rc) throws GameActionException {
        super(rc);
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        theirHQPossibilities = calculateEnemyHQLocations(myHQ);
        hqTry = 0;
        pathFinder = new PathFinder(rc);
    }

    public void run(int roundNum) throws GameActionException {
        if ((goal == null || goal == Goal.IDLE) && roundNum - createdOnRound < GameConstants.INITIAL_COOLDOWN_TURNS) {
            goal = checkInitialGoal(rc.getLocation(), roundNum);
        }

        // Without the while loop we waste turns changing goals
        while (rc.isReady() && goal != null && goal != Goal.IDLE) {
            switch (goal) {
                case FIND_ENEMY_HQ:
                    findEnemyHQ();
                    break;
                case GO_TO_ENEMY_HQ:
                    goToEnemyHQ();
                    break;
                case GO_TO_MY_HQ:
                    goToMyHQ();
                    break;
                case ATTACK_UNITS:
                    attackUnits();
                    break;
                case DEFEND_HQ:
                    defendHQ();
                    break;
                case PICK_UP_UNIT:
                    pickUpUnit();
                    break;
                case DESTROY_UNIT:
                    destroyUnit();
                    break;
                default:
                    throw new IllegalStateException("Invalid goal for landscaper " + goal);
            }
        }
    }

    private void goToEnemyHQ() throws GameActionException {
        if (pathFinder.getGoal() != theirHQ) {
            pathFinder.setGoal(theirHQ);
        }
        pathFinder.move(false, true);
        if (rc.getLocation().distanceSquaredTo(theirHQ) < 9) {
            goal = Goal.ATTACK_UNITS;
            return;
        }
    }

    private void goToMyHQ() throws GameActionException {
        if (pathFinder.getGoal() != myHQ) {
            pathFinder.setGoal(myHQ);
        }
        pathFinder.move(false, true);
        if (rc.getLocation().distanceSquaredTo(myHQ) < 9) {
            goal = Goal.DEFEND_HQ;
            return;
        }
    }

    private void attackUnits() throws GameActionException {
        int min = Integer.MAX_VALUE;
        RobotInfo robotToAttack = null;
        RobotInfo[] robots = rc.senseNearbyRobots(24, rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            if (robot.type != RobotType.LANDSCAPER && robot.type != RobotType.MINER) {
                continue;
            }

            int distanceTo = rc.getLocation().distanceSquaredTo(robot.location);
            if (distanceTo < min) {
                min = distanceTo;
                robotToAttack = robot;
            }
        }

        if (robotToAttack == null) {
            goal = Goal.GO_TO_ENEMY_HQ;
            return;
        }

        target_id = robotToAttack.ID;
        goal = Goal.PICK_UP_UNIT;
    }

    private void defendHQ() throws GameActionException {
        int min = Integer.MAX_VALUE;
        RobotInfo robotToAttack = null;
        RobotInfo[] robots = rc.senseNearbyRobots(24, rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            if (robot.type != RobotType.LANDSCAPER && robot.type != RobotType.MINER) {
                continue;
            }

            int distanceTo = rc.getLocation().distanceSquaredTo(robot.location);
            if (distanceTo < min) {
                min = distanceTo;
                robotToAttack = robot;
            }
        }

        if (robotToAttack == null) {
            goal = Goal.GO_TO_MY_HQ;
            return;
        }

        target_id = robotToAttack.ID;
        goal = Goal.PICK_UP_UNIT;
    }

    private void pickUpUnit() throws GameActionException {
        if (!rc.canSenseRobot(target_id)) {
            goal = Goal.GO_TO_ENEMY_HQ;
            return;
        }

        pathFinder.setGoal(rc.senseRobot(target_id).getLocation());
        if (rc.isReady()) {
            pathFinder.move(false, true);
            if (pathFinder.isFinished()) {
                //TODO: Drone will see unit picked up by other drone and try to follow it
                // We need to figure out if the target is being carried by a drone before other drone is adjacent.
                if (!rc.canPickUpUnit(target_id)) {
                    goal = Goal.GO_TO_ENEMY_HQ;
                    return;
                }
                rc.pickUpUnit(target_id);
                //Set this to be the closest location of water
                pathFinder.setGoal(new MapLocation(35, 18));
                goal = Goal.DESTROY_UNIT;
            }
        }
    }

    private void destroyUnit() throws GameActionException {
        if (pathFinder.isFinished()) {
            Direction dropDirection = findUnoccupiedDropLocation();
            if (dropDirection == null) {
                return;
            }
            rc.dropUnit(dropDirection);
            goal = Goal.GO_TO_ENEMY_HQ;
        } else {
            pathFinder.move(false, true);
        }
    }

    private Direction findUnoccupiedDropLocation() throws GameActionException {
        MapLocation loc = rc.getLocation();
        for (Direction dir: Direction.values()) {
            MapLocation newLoc = loc.add(dir);
            if (rc.senseFlooding(newLoc) && !rc.isLocationOccupied(newLoc)) {
                return dir;
            }
        }

        return null;
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
        } else if (pathFinder.isFinished() || canSeeEnemyHQ()) {
            goal = Goal.ATTACK_UNITS;
            theirHQ = theirHQPossibilities[hqTry-1];
        }
        if (rc.isReady()) {
            pathFinder.move(false, true);
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

}
