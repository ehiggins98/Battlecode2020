package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.*;
import jers.PathFinder;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class DeliveryDrone extends Robot {
    // Map is rotationally, horizontally, or vertically symmetric, so we don't know for sure where the HQ is.
    private MapLocation[] theirHQPossibilities;
    private int hqTry;
    private MapLocation theirHQ;
    private PathFinder pathFinder;
    private int target_id;
    private ArrayList<MapLocation> waterLocations;
    private ArrayList<Message> waterFoundMessages;
    int startupLastRoundChecked = 0;
    private Goal initialGoal;
    private Random random;
    private MapLocation pickupLandscaper;
    private int roundLastUpdated;

    public DeliveryDrone(RobotController rc) throws GameActionException {
        super(rc);
        hqTry = 0;
        pathFinder = new PathFinder(rc);
        waterLocations = new ArrayList<>();
        waterFoundMessages = new ArrayList<>();
        goal = Goal.STARTUP;
        pickupLandscaper = null;
        roundLastUpdated = rc.getRoundNum();
    }

    public void run(int roundNum) throws GameActionException {
        if (goal == Goal.STARTUP) {
            startup(roundNum);
            if (goal == Goal.STARTUP) {
                return;
            }
        }

        Goal lastGoal = null;

        // Without the while loop we waste turns changing goals
        while(rc.isReady() && goal != null && lastGoal != goal) {
            lastGoal = goal;
            MapLocation water = checkWater();
            if (water != null) {
                waterLocations.add(water);
                waterFoundMessages.add(new WaterFoundMessage(new RobotType[]{RobotType.DELIVERY_DRONE}, Goal.ALL, water));
            }
            switch (goal) {
                case IDLE:
                    break;
                case GET_INITIAL_GOAL:
                    interactWithBlockchain(roundNum);
                    break;
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
                case FIND_WATER:
                    findWater();
                    break;
                case DESTROY_UNIT:
                    destroyUnit();
                    break;
                case PICK_UP_LANDSCAPER:
                    pickUpLandscaper(roundNum);
                    break;
                default:
                    throw new IllegalStateException("Invalid goal for landscaper " + goal);
            }
        }
    }

    private MapLocation checkWater() throws GameActionException {
        double radius = Math.sqrt(rc.getCurrentSensorRadiusSquared());
        MapLocation closest = null;
        int closestDist = Integer.MAX_VALUE;

        for (int dx = -1 * (int) radius; dx < radius; dx++) {
            for (int dy = -1 * (int) radius; dy < radius; dy++) {
                MapLocation check = rc.getLocation().translate(dx, dy);
                if (rc.canSenseLocation(check) && rc.senseFlooding(check) && (dx*dx + dy*dy < closestDist)) {
                    closestDist = dx*dx + dy*dy;
                    closest = check;
                }
            }
        }

        return closest;
    }

    private void startup(int roundNum) throws GameActionException {
        if (startupLastRoundChecked >= roundNum - 2) {
            goal = initialGoal != null ? initialGoal : Goal.GET_INITIAL_GOAL;
            return;
        }

        while (startupLastRoundChecked < roundNum - 1 && Clock.getBytecodesLeft() > 600) {
            ArrayList<Message> messages = transactor.getBlock(++startupLastRoundChecked, goal);
            for (Message m : messages) {
                switch (m.getMessageType()) {
                    case ROBOT_BUILT:
                        RobotBuiltMessage robotBuiltMessage = (RobotBuiltMessage) m;
                        if (robotBuiltMessage.getRobotType() == RobotType.HQ) {
                            myHQ = robotBuiltMessage.getRobotLocation();
                            theirHQPossibilities = calculateEnemyHQLocations(myHQ);
                        }
                        break;
                    case WATER_FOUND:
                        WaterFoundMessage waterFoundMessage = (WaterFoundMessage) m;
                        waterLocations.add(waterFoundMessage.getLocation());
                        break;
                    case INITIAL_GOAL:
                        InitialGoalMessage initialGoalMessage = (InitialGoalMessage) m;
                        if (initialGoalMessage.getRoundCreated() == createdOnRound &&
                                initialGoalMessage.getInitialLocation().equals(rc.getLocation())) {
                            initialGoal = initialGoalMessage.getInitialGoal();
                        }
                        break;
                }
            }
        }
    }

    private void goToEnemyHQ() throws GameActionException {
        if (!pathFinder.getGoal().equals(theirHQ)) {
            pathFinder.setGoal(theirHQ);
        }
        pathFinder.move(false, true);
        if (rc.getLocation().distanceSquaredTo(theirHQ) <= 9) {
            if (rc.isCurrentlyHoldingUnit()) {
                rc.dropUnit(findUnoccupiedDropLocation(false));
            }
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

        if (robotToAttack != null) {
            target_id = robotToAttack.ID;
            goal = Goal.PICK_UP_UNIT;
        }
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
        if (robotToAttack != null) {
            target_id = robotToAttack.ID;
            goal = Goal.PICK_UP_UNIT;
        }
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
                if (!waterLocations.isEmpty()) {
                    waterLocations.sort(new ClosestLocComparator(rc.getLocation()));
                    pathFinder.setGoal(waterLocations.get(0));
                    goal = Goal.DESTROY_UNIT;
                } else {
                    goal = Goal.FIND_WATER;
                }
            }
        }
    }

    public MapLocation lookForLandscaper(int roundNum) throws GameActionException {
        for (int i = roundLastUpdated-1; i < roundNum; i++) {
            ArrayList<Message> messages = transactor.getBlock(i, goal);
            for (Message message : messages) {
                System.out.println("Reading this");
                if (message.getMessageType().equals(MessageType.LANDSCAPER_LOCATED)) {
                    System.out.println("Was a landscaper");
                    return((LandscaperLocationMessage) message).getLocation();
                }
            }
        }


        return pickupLandscaper;
    }
    private void pickUpLandscaper(int roundNum) throws GameActionException {
        if (pickupLandscaper == null)
        {
            pickupLandscaper = lookForLandscaper(roundNum);
            pathFinder.setGoal(pickupLandscaper);
            return;
        }

        if (rc.isReady()) {
            pathFinder.move(false, true);
            if (pathFinder.isFinished()) {
                //TODO: Drone will see unit picked up by other drone and try to follow it
                // We need to figure out if the target is being carried by a drone before other drone is adjacent.
                rc.pickUpUnit(rc.senseRobotAtLocation(pickupLandscaper).getID());
                goal = Goal.FIND_ENEMY_HQ;
            }
        }

    }

    private void findWater() throws GameActionException {
        if (pathFinder.getGoal() == null || pathFinder.isFinished()) {
            MapLocation newGoal = new MapLocation(random.nextInt(rc.getMapWidth()), random.nextInt(rc.getMapHeight()));
            pathFinder.setGoal(newGoal);
        }

        pathFinder.move(false, true);
        MapLocation water = checkWater();
        if (water != null) {
            waterLocations.add(water);
            waterFoundMessages.add(new WaterFoundMessage(new RobotType[]{RobotType.DELIVERY_DRONE}, Goal.ALL, water));
            pathFinder.setGoal(water);
            goal = Goal.DESTROY_UNIT;
        }
    }

    private void destroyUnit() throws GameActionException {
        if (pathFinder.isFinished()) {
            Direction dropDirection = findUnoccupiedDropLocation(true);
            if (dropDirection == null) {
                return;
            }
            rc.dropUnit(dropDirection);
            goal = Goal.GO_TO_ENEMY_HQ;
        } else {
            boolean success = pathFinder.move(false, true);
            if (!success) {
                System.out.println(success);
            }
        }
    }

    private Direction findUnoccupiedDropLocation(boolean toDestroy) throws GameActionException {
        MapLocation loc = rc.getLocation();
        for (Direction dir: Direction.values()) {
            MapLocation newLoc = loc.add(dir);
            if (toDestroy) {
                if (rc.senseFlooding(newLoc) && !rc.isLocationOccupied(newLoc)) {
                    return dir;
                }
            }
            else {
                if (!rc.isLocationOccupied(newLoc)) {
                    return dir;
                }
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

    private void interactWithBlockchain(int roundNum) throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(roundNum - 1, goal);

        for (Message m : messages) {
            switch (m.getMessageType()) {
                case ROBOT_BUILT:
                    RobotBuiltMessage robotBuiltMessage = (RobotBuiltMessage) m;
                    if (myHQ == null && robotBuiltMessage.getRobotType() == RobotType.HQ) {
                        myHQ = robotBuiltMessage.getRobotLocation();
                        theirHQPossibilities = calculateEnemyHQLocations(myHQ);
                    }
                    break;
                case INITIAL_GOAL:
                    InitialGoalMessage initialGoalMessage = (InitialGoalMessage) m;
                    if (initialGoalMessage.getRoundCreated() == createdOnRound &&
                            initialGoalMessage.getInitialLocation().equals(rc.getLocation())) {
                        initialGoal = initialGoalMessage.getInitialGoal();
                    }
                    break;
                case WATER_FOUND:
                    WaterFoundMessage waterFoundMessage = (WaterFoundMessage) m;
                    waterLocations.add(waterFoundMessage.getLocation());
                    break;
            }
        }

        if (!waterFoundMessages.isEmpty() && transactor.submitTransaction(waterFoundMessages.get(0))) {
            waterFoundMessages.remove(0);
        }
    }

}
