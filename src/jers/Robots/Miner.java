package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.*;
import jers.PathFinder;
import jers.Transactor;

import java.util.*;

import static battlecode.common.RobotType.*;
import static jers.Constants.*;

public class Miner extends Robot {
    private PathFinder pathFinder;
    private Transactor transactor;
    private List<RobotInfo> buildings;
    private List<MapLocation> soupLocations;
    private HashSet<MapLocation> sharedSoupLocations;
    private Random random;
    private int startupLastRoundChecked = 0;
    private Goal initialGoal;
    private MapLocation theirHQ;
    private MapLocation[] theirHQPossibilities;
    private int hqTry;
    private Queue<Message> messageQueue;
    private int dronesBuilt = 0;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        buildings = new ArrayList<>();
        pathFinder = new PathFinder(rc);
        transactor = new Transactor(rc);
        soupLocations = new ArrayList<>();
        messageQueue = new LinkedList<>();
        sharedSoupLocations = new HashSet<>();
        random = new Random();
        goal = Goal.STARTUP;
    }

    /**
     * Basically just mine and refine soup until the end of the game. If the miner can't see any soup, it starts
     * a semi-random walk around the map until it finds some, and then it mines there.
     * @param roundNum The current round.
     * @throws GameActionException
     * @throws IllegalStateException
     */
    @Override
    public void run(int roundNum) throws GameActionException, IllegalStateException {
        if (goal == Goal.STARTUP) {
            startUp(roundNum);
            if (Clock.getBytecodesLeft() < 600 || goal == Goal.STARTUP) {
                return;
            }
        }

        Goal lastGoal = null;
        readBlockchain(roundNum);

        if (initialGoal != Goal.FIND_ENEMY_HQ && !hasBuiltUnit(FULFILLMENT_CENTER) && rc.getTeamSoup() >= FULFILLMENT_CENTER.cost &&
                (hasBuiltUnit(NET_GUN) || rc.getTeamSoup() - FULFILLMENT_CENTER.cost >= NET_GUN.cost)) {
            goal = Goal.BUILD_FULFILLMENT_CENTER;
        } else if (initialGoal != Goal.FIND_ENEMY_HQ && !hasBuiltUnit(DESIGN_SCHOOL) && rc.getTeamSoup() >= DESIGN_SCHOOL.cost &&
                (hasBuiltUnit(NET_GUN) || rc.getTeamSoup() - DESIGN_SCHOOL.cost >= NET_GUN.cost) &&
                dronesBuilt >= INITIAL_DEFENDING_DRONES) {
            goal = Goal.BUILD_DESIGN_SCHOOL;
        }

        while (rc.isReady() && lastGoal != goal) {
            lastGoal = goal;
            switch (goal) {
                case IDLE:
                    break;
                case FIND_NEW_SOUP:
                    findNewSoup();
                    break;
                case MINE:
                    mine();
                    break;
                case REFINE:
                    refine();
                    break;
                case EXPLORE:
                    explore();
                    break;
                case FIND_ENEMY_HQ:
                    findEnemyHQ();
                    break;
                case GO_TO_ENEMY_HQ:
                    goToEnemyHQ();
                    break;
                case BUILD_REFINERY:
                    makeBuilding(REFINERY, Goal.FIND_NEW_SOUP, false);
                    break;
                case BUILD_NET_GUN:
                    makeBuilding(NET_GUN, Goal.IDLE, true);
                    break;
                case BUILD_DESIGN_SCHOOL:
                    makeBuilding(DESIGN_SCHOOL, Goal.FIND_NEW_SOUP, true);
                    break;
                case BUILD_FULFILLMENT_CENTER:
                    makeBuilding(FULFILLMENT_CENTER, Goal.FIND_NEW_SOUP, true);
                    break;
                default:
                    throw new IllegalStateException("Invalid goal for miner " + goal);
            }
        }

        writeBlockchain();
    }

    private void startUp(int roundNum) throws GameActionException {
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
                        switch (robotBuiltMessage.getRobotType()) {
                            case REFINERY:
                            case DESIGN_SCHOOL:
                            case FULFILLMENT_CENTER:
                            case NET_GUN:
                                buildings.add(new RobotInfo(-1, rc.getTeam(), robotBuiltMessage.getRobotType(), robotBuiltMessage.getRobotLocation()));
                                break;
                            case DELIVERY_DRONE:
                                dronesBuilt++;
                                break;
                            case HQ:
                                myHQ = robotBuiltMessage.getRobotLocation();
                                theirHQPossibilities = calculateEnemyHQLocations(myHQ);
                                buildings.add(new RobotInfo(-1, rc.getTeam(), HQ, robotBuiltMessage.getRobotLocation()));
                                break;
                        }
                        break;
                    case SOUP_FOUND:
                        SoupFoundMessage soupFoundMessage = (SoupFoundMessage) m;
                        soupLocations.add(soupFoundMessage.getLocation());
                        break;
                    case INITIAL_GOAL:
                        InitialGoalMessage initialGoalMessage = (InitialGoalMessage) m;
                        if (initialGoalMessage.getInitialLocation().equals(rc.getLocation()) &&
                                initialGoalMessage.getRoundCreated() == createdOnRound) {
                            initialGoal = initialGoalMessage.getInitialGoal();
                        }
                        break;
                }
            }
        }
    }

    // Execute the idle strategy - if we know where soup is, go there, otherwise look for soup in the vicinity, and
    // otherwise explore.
    private void findNewSoup() throws GameActionException {
        MapLocation loc = findSoup();
        if (loc != null && !soupLocations.contains(loc)) {
            soupLocations.add(loc);
        }

        if (!soupLocations.isEmpty()) {
            goal = setMiningGoal();
        } else {
            goal = Goal.EXPLORE;
        }
    }

    // Execute the mine strategy - go to soup, mine it until we're full, then refine.
    private void mine() throws GameActionException {
        pathFinder.move(false, false, myHQ);

        if (rc.getSoupCarrying() >= RobotType.MINER.soupLimit && rc.isReady()) {
            goal = Goal.REFINE;
        } else {
            if (rc.canSenseLocation(soupLocations.get(0)) && rc.senseSoup(soupLocations.get(0)) == 0) {
                goal = Goal.FIND_NEW_SOUP;
                soupLocations.remove(0);
            }

            if (soupLocations.isEmpty()) {
                goal = Goal.EXPLORE;
            } else if ((rc.getLocation().equals(soupLocations.get(0)) || rc.getLocation().isAdjacentTo(soupLocations.get(0)))
                    && rc.canMineSoup(rc.getLocation().directionTo(soupLocations.get(0)))) {
                if (!sharedSoupLocations.contains(soupLocations.get(0)) && farFromAllSharedSoup(soupLocations.get(0))) {
                    sharedSoupLocations.add(soupLocations.get(0));
                    messageQueue.add(new SoupFoundMessage(new RobotType[]{MINER}, Goal.ALL, soupLocations.get(0)));
                }

                rc.mineSoup(rc.getLocation().directionTo(soupLocations.get(0)));
            } else if (pathFinder.isFinished()) {
                goal = Goal.FIND_NEW_SOUP;
            }
        }
    }

    // Go to the HQ and dump all our soup in, then go back to mining.
    private void refine() throws GameActionException {
        for (RobotInfo building : buildings) {
            if (building.getType() != REFINERY && building.getType() != HQ) {
                continue;
            }

            if (rc.getLocation().isAdjacentTo(building.getLocation())) {
                Direction dirToRefinery = rc.getLocation().directionTo(building.getLocation());
                if (rc.canDepositSoup(dirToRefinery)) {
                    rc.depositSoup(dirToRefinery, rc.getSoupCarrying());
                    goal = Goal.FIND_NEW_SOUP;
                    break;
                }
            }
        }

        if (pathFinder.getGoal() == null || pathFinder.isFinished()) {
            MapLocation refinery = findOrBuildRefinery();
            if (refinery != null) {
                pathFinder.setGoal(getOpenTileAdjacent(refinery, refinery.directionTo(rc.getLocation()), Constants.directions, false));
            }
        }
        pathFinder.move(false, false, myHQ);
    }

    // Walk semi-randomly around the map until we see soup. This just generates random goals at most MAX_EXPLORE_DELTA
    // tiles away from our current location and goes there. If there's no soup visible, it generates another goal.
    private void explore() throws GameActionException {
        boolean success = pathFinder.move(false, false, myHQ);
        if (!success || pathFinder.isFinished()) {
            pathFinder.setGoal(getRandomGoal());
        }

        if (soupLocations.isEmpty()) {
            MapLocation loc = findSoup();
            if (loc != null) {
                soupLocations.add(loc);
            }
        }

        if (!soupLocations.isEmpty()) {
            goal = setMiningGoal();
        }
    }

    // Find the enemy HQ. We know the map is horizontally, vertically, or rotationally symmetric, so we have only
    // three locations where it can be. Thus, we try these possibilities until we find the HQ, then switch to
    // the attack goal.
    private void findEnemyHQ() throws GameActionException {
        if (theirHQ != null) {
            goal = Goal.GO_TO_ENEMY_HQ;
            return;
        }

        if (pathFinder.getGoal() == null || (pathFinder.isFinished() && !canSeeEnemyHQ())) {
            if (hqTry < 3) {
                pathFinder.setGoal(theirHQPossibilities[hqTry].subtract(rc.getLocation().directionTo(theirHQPossibilities[hqTry])));
                hqTry++;
            } else {
                // We can't get to the enemy HQ
                goal = Goal.FIND_NEW_SOUP;
            }
        } else if (pathFinder.isFinished() && canSeeEnemyHQ()) {
            if (rc.getLocation().isAdjacentTo(theirHQPossibilities[hqTry - 1])) {
                if (!hasBuiltUnit(NET_GUN)) {
                    goal = Goal.BUILD_NET_GUN;
                }

                theirHQ = theirHQPossibilities[hqTry-1];
                messageQueue.add(new HqFoundMessage(new RobotType[]{RobotType.LANDSCAPER, RobotType.DELIVERY_DRONE,
                        RobotType.FULFILLMENT_CENTER, RobotType.DESIGN_SCHOOL}, Goal.ALL, theirHQ));
            } else {
                MapLocation newGoal = getOpenTileAdjacent(theirHQPossibilities[hqTry-1],
                        theirHQPossibilities[hqTry-1].directionTo(rc.getLocation()), Constants.directions, false);

                if (newGoal == null) {
                    goal = Goal.FIND_NEW_SOUP;
                } else {
                    pathFinder.setGoal(newGoal);
                }
            }
        }
        if (rc.isReady()) {
            pathFinder.move(false, false, myHQ);
        }
    }

    private void goToEnemyHQ() throws GameActionException {
        boolean nearHQ = canSeeEnemyHQ();
        if (!theirHQ.equals(pathFinder.getGoal()) && !canSeeEnemyHQ()) {
            pathFinder.setGoal(theirHQ);
        } else if (!pathFinder.isFinished() && nearHQ) {
            MapLocation goal = getOpenTileAdjacent(theirHQ, theirHQ.directionTo(rc.getLocation()), Constants.directions, false);
            if (goal != null) {
                pathFinder.setGoal(goal);
            }
        } else if (nearHQ && !hasBuiltUnit(NET_GUN)) {
            goal = Goal.BUILD_NET_GUN;
        }

        pathFinder.move(false, false, myHQ);
    }

    // Stuff that needs to be done regardless of our goal.
    private void readBlockchain(int roundNum) throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(roundNum - 1, goal);
        for (Message m : messages) {
            switch (m.getMessageType()) {
                case ROBOT_BUILT:
                    RobotBuiltMessage message = (RobotBuiltMessage) m;
                    switch (message.getRobotType()) {
                        case HQ:
                            myHQ = message.getRobotLocation();
                            theirHQPossibilities = calculateEnemyHQLocations(myHQ);
                            buildings.add(new RobotInfo(-1, rc.getTeam(), HQ, message.getRobotLocation()));
                            break;
                        case REFINERY:
                        case DESIGN_SCHOOL:
                        case FULFILLMENT_CENTER:
                        case NET_GUN:
                            buildings.add(new RobotInfo(-1, rc.getTeam(), message.getRobotType(), message.getRobotLocation()));
                            break;
                        case DELIVERY_DRONE:
                            dronesBuilt++;
                    }
                    break;
                case SOUP_FOUND:
                    soupLocations.add(((SoupFoundMessage) m).getLocation());
                    break;
                case INITIAL_GOAL:
                    InitialGoalMessage initialGoalMessage = (InitialGoalMessage) m;
                    if (initialGoalMessage.getInitialLocation().equals(rc.getLocation()) &&
                            initialGoalMessage.getRoundCreated() == createdOnRound) {
                        initialGoal = initialGoalMessage.getInitialGoal();
                        goal = initialGoal;
                    }
                    break;
                case HQ_FOUND:
                    HqFoundMessage hqFoundMessage = (HqFoundMessage) m;
                    theirHQ = hqFoundMessage.getLocation();
                    break;
            }
        }
    }

    private void writeBlockchain() throws GameActionException {
        if (!messageQueue.isEmpty()) {
            boolean success = transactor.submitTransaction(messageQueue.peek());
            if (success) {
                messageQueue.poll();
            }
        }
    }

    // Make a building. This ensures we don't put the building in water, that it's not on top of soup, and that it's far enough away
    // from the HQ (just so the building doesn't interfere with building a wall around the HQ.
    private MapLocation makeBuilding(RobotType type, Goal nextGoal, boolean buildOne) throws GameActionException {
        if (buildOne && hasBuiltUnit(type)) {
            goal = nextGoal;
            return null;
        }

        for (Direction d : Direction.allDirections()) {
            MapLocation buildAt = rc.getLocation().add(d);
            if (rc.canBuildRobot(type, d) && !rc.senseFlooding(buildAt) && rc.senseSoup(buildAt) == 0 && buildAt.distanceSquaredTo(myHQ) >= 9) {
                rc.buildRobot(type, d);
                messageQueue.add(new RobotBuiltMessage(new RobotType[]{MINER, FULFILLMENT_CENTER, DESIGN_SCHOOL}, Goal.ALL, buildAt, type));
                goal = nextGoal;
            }
        }

        return null;
    }

    // Find the nearest visible soup.
    private MapLocation findSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int minDist = Integer.MAX_VALUE;
        MapLocation argClosest = null;

        for (int x = loc.x - 5; x <= loc.x + 5; x++) {
            for (int y = loc.y - 5; y <= loc.y + 5; y++) {
                MapLocation newLoc = new MapLocation(x, y);
                if (rc.canSenseLocation(newLoc) && rc.senseSoup(newLoc) > 0 && !rc.senseFlooding(newLoc) &&
                        loc.distanceSquaredTo(newLoc) < minDist) {
                    boolean canMine = !rc.isLocationOccupied(newLoc) || newLoc.equals(loc) ||
                            getOpenTileAdjacent(newLoc, newLoc.directionTo(loc), Constants.directions, true) != null;

                    if (canMine) {
                        argClosest = newLoc;
                        minDist = loc.distanceSquaredTo(newLoc);
                    }
                }
            }
        }

        return argClosest;
    }

    // Get a random goal, used for explore mode.
    private MapLocation getRandomGoal() {
        int newX = random.nextInt(rc.getMapWidth());
        int newY = random.nextInt(rc.getMapHeight());
        return new MapLocation(newX, newY);
    }

    private Goal setMiningGoal() throws GameActionException {
        soupLocations.sort(new ClosestLocComparator(rc.getLocation()));
        while (!soupLocations.isEmpty() && rc.canSenseLocation(soupLocations.get(0)) && rc.senseSoup(soupLocations.get(0)) == 0) {
            soupLocations.remove(0);
        }

        if (soupLocations.isEmpty()) {
            return Goal.FIND_NEW_SOUP;
        } else if (rc.canSenseLocation(soupLocations.get(0)) &&
                (!rc.isLocationOccupied(soupLocations.get(0)) || soupLocations.get(0).equals(rc.getLocation())) ||
                !rc.canSenseLocation(soupLocations.get(0))) {
            pathFinder.setGoal(soupLocations.get(0));
        } else {
            pathFinder.setGoal(getOpenTileAdjacent(soupLocations.get(0), soupLocations.get(0).directionTo(rc.getLocation()), Constants.directions, true));
        }

        return Goal.MINE;
    }

    private MapLocation findOrBuildRefinery() throws GameActionException {
        MapLocation closest = null;
        int minDist = Integer.MAX_VALUE;
        boolean hasRefinery = false;

        for (RobotInfo building : buildings) {
            if (building.getType() == REFINERY) {
                hasRefinery = true;
            }
        }

        for (RobotInfo building : buildings) {
            if (building.getType() != REFINERY && building.getType() != HQ) {
                continue;
            }

            if ((!hasRefinery || !building.getLocation().equals(myHQ)) &&
                    rc.getLocation().distanceSquaredTo(building.getLocation()) < minDist) {
                minDist = rc.getLocation().distanceSquaredTo(building.getLocation());
                closest = building.getLocation();
            }
        }
/*
        if (rc.getLocation().distanceSquaredTo(closest) > Constants.FAR_THRESHOLD_RADIUS_SQUARED &&
                landscapersBuilt >= INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL) {
            if (rc.getTeamSoup() < REFINERY.cost) {
                messageQueue.add(new NeedRefineryMessage(new RobotType[]{DESIGN_SCHOOL, FULFILLMENT_CENTER}, Goal.ALL));
            } else {
                goal = Goal.BUILD_REFINERY;
            }
        }*/

        return closest;
    }

    private boolean farFromAllSharedSoup(MapLocation soupLoc) {
        for (MapLocation soup : sharedSoupLocations) {
            if (soupLoc.distanceSquaredTo(soup) <= Constants.FAR_THRESHOLD_RADIUS_SQUARED) {
                return false;
            }
        }

        return true;
    }

    // Check if we can see the enemy HQ.
    private boolean canSeeEnemyHQ() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo info : robots) {
            if (info.getType() == RobotType.HQ && info.getTeam() == rc.getTeam().opponent()) {
                return true;
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

    private boolean hasBuiltUnit(RobotType type) {
        for (RobotInfo b : buildings) {
            if (b.getType() == type) {
                return true;
            }
        }

        return false;
    }
}
