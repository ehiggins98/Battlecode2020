package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RobotBuiltMessage;
import jers.Messages.SoupFoundMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.*;

import static battlecode.common.RobotType.*;
import static jers.Constants.INITIAL_ATTACKING_LANDSCAPERS;
import static jers.Constants.LANDSCAPERS_FOR_WALL;

public class Miner extends Robot {
    // Distance needed to be considered "far" from HQ. We broadcast soup locations
    // if found beyond this distance, and build a refinery if soup is found at least
    // this far from HQ.
    private final int FAR_THRESHOLD_RADIUS_SQUARED = 25;

    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation designSchoolLocation = null;
    private List<MapLocation> refineryLocations = null;
    private List<MapLocation> fulfillmentLocations = null;
    private List<MapLocation> soupLocations = null;
    private List<RobotBuiltMessage> robotBuiltMessages;
    private HashSet<MapLocation> sharedSoupLocations;
    private Random random;
    private int landscapersBuilt;
    private int startupLastRoundChecked = 0;

    boolean soupFoundTransactionNeeded = false;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        refineryLocations = new ArrayList<>();
        fulfillmentLocations = new ArrayList<>();
        pathFinder = new PathFinder(rc);
        transactor = new Transactor(rc);
        soupLocations = new ArrayList<>();
        robotBuiltMessages = new ArrayList<>();
        sharedSoupLocations = new HashSet<>();
        random = new Random();
        goal = Goal.STARTUP;
        System.out.println("New miner");
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
        allGoals(roundNum);

        Goal lastGoal = null;

        while (rc.isReady() && lastGoal != goal) {
            //System.out.println(goal);
            lastGoal = goal;
            switch (goal) {
                case IDLE:
                    idle();
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
                case BUILD_REFINERY:
                    buildRefinery();
                    break;
                default:
                    throw new IllegalStateException("Invalid goal for miner " + goal);
            }
        }
    }

    private void startUp(int roundNum) throws GameActionException {
        if (startupLastRoundChecked >= roundNum - 2) {
            goal = Goal.IDLE;
            return;
        }

        while (startupLastRoundChecked < roundNum - 1 && Clock.getBytecodesLeft() > 600) {
            ArrayList<Message> messages = transactor.getBlock(++startupLastRoundChecked, goal);
            for (Message m : messages) {
                if (m.getMessageType() == MessageType.ROBOT_BUILT) {
                    RobotBuiltMessage message = (RobotBuiltMessage) m;
                    switch (message.getRobotType()) {
                        case REFINERY:
                            refineryLocations.add(message.getRobotLocation());
                            break;
                        case DESIGN_SCHOOL:
                            designSchoolLocation = message.getRobotLocation();
                            break;
                        case FULFILLMENT_CENTER:
                            fulfillmentLocations.add(message.getRobotLocation());
                            break;
                        case LANDSCAPER:
                            landscapersBuilt++;
                            break;
                        case HQ:
                            myHQ = message.getRobotLocation();
                            refineryLocations.add(myHQ);
                            break;
                    }
                } else if (m.getMessageType() == MessageType.SOUP_FOUND) {
                    SoupFoundMessage message = (SoupFoundMessage) m;
                    soupLocations.add(message.getLocation());
                }
            }
        }
    }

    // Execute the idle strategy - if we know where soup is, go there, otherwise look for soup in the vicinity, and
    // otherwise explore.
    private void idle() throws GameActionException {
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
        pathFinder.move(false, false);

        if (rc.getSoupCarrying() >= RobotType.MINER.soupLimit && rc.isReady()) {
            goal = Goal.REFINE;
        } else {
            if (rc.canSenseLocation(soupLocations.get(0)) && rc.senseSoup(soupLocations.get(0)) == 0) {
                goal = Goal.IDLE;
                soupLocations.remove(0);
            }

            if (soupLocations.isEmpty()) {
                goal = Goal.EXPLORE;
            } else if ((rc.getLocation().equals(soupLocations.get(0)) || rc.getLocation().isAdjacentTo(soupLocations.get(0)))
                    && rc.canMineSoup(rc.getLocation().directionTo(soupLocations.get(0)))) {
                if (!sharedSoupLocations.contains(soupLocations.get(0)) && farFromAllSharedSoup(soupLocations.get(0))) {
                    sharedSoupLocations.add(soupLocations.get(0));
                    soupFoundTransactionNeeded = true;
                }

                rc.mineSoup(rc.getLocation().directionTo(soupLocations.get(0)));
                if (!rc.getLocation().equals(soupLocations.get(0))) {
                    rc.setIndicatorLine(rc.getLocation(), soupLocations.get(0), 0, 0, 255);
                } else {
                    rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
                }
            }
        }
    }

    // Go to the HQ and dump all our soup in, then go back to mining.
    private void refine() throws GameActionException {
        for (MapLocation refinery : refineryLocations) {
            if (rc.getLocation().isAdjacentTo(refinery)) {
                Direction dirToRefinery = rc.getLocation().directionTo(refinery);
                if (rc.canDepositSoup(dirToRefinery)) {
                    rc.depositSoup(dirToRefinery, rc.getSoupCarrying());
                    goal = Goal.IDLE;
                    break;
                }
            }
        }

        if (pathFinder.getGoal() == null || pathFinder.isFinished()) {
            MapLocation refinery = findOrBuildRefinery();
            pathFinder.setGoal(getOpenTileAdjacent(refinery, refinery.directionTo(rc.getLocation()), Constants.directions, false));
        }
        pathFinder.move(false, false);
    }

    // Walk semi-randomly around the map until we see soup. This just generates random goals at most MAX_EXPLORE_DELTA
    // tiles away from our current location and goes there. If there's no soup visible, it generates another goal.
    private void explore() throws GameActionException {
        boolean success = pathFinder.move(false, false);
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

    // Go far enough away from the HQ and build a refinery.
    private void buildRefinery() throws GameActionException {
        MapLocation builtAt = makeBuilding(REFINERY);
        if (builtAt != null) {
            refineryLocations.add(builtAt);
            goal = rc.getSoupCarrying() >= RobotType.MINER.soupLimit ? Goal.REFINE : Goal.MINE;
            robotBuiltMessages.add(new RobotBuiltMessage(new RobotType[]{RobotType.MINER}, Goal.ALL, builtAt, REFINERY));
        }
    }

    // Stuff that needs to be done regardless of our goal.
    private void allGoals(int roundNum) throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(roundNum - 1, goal);
        for (Message m : messages) {
            switch (m.getMessageType()) {
                case ROBOT_BUILT:
                    RobotBuiltMessage message = (RobotBuiltMessage) m;
                    switch (message.getRobotType()) {
                        case HQ:
                            myHQ = message.getRobotLocation();
                            refineryLocations.add(myHQ);
                            break;
                        case REFINERY:
                            System.out.println("Found refinery");
                            refineryLocations.add(message.getRobotLocation());
                            if (goal == Goal.BUILD_REFINERY) {
                                goal = Goal.IDLE;
                            }
                            break;
                        case DESIGN_SCHOOL:
                            System.out.println("Found design school");
                            designSchoolLocation = message.getRobotLocation();
                            break;
                        case FULFILLMENT_CENTER:
                            System.out.println("Found fulfillment center");
                            fulfillmentLocations.add(message.getRobotLocation());
                            break;
                        case LANDSCAPER:
                            landscapersBuilt++;
                            break;
                    }
                    break;
                case SOUP_FOUND:
                    soupLocations.add(((SoupFoundMessage) m).getLocation());
                    break;
            }
        }

        if (rc.getTeamSoup() > REFINERY.cost && refineryLocations.size() == 1) {
            goal = Goal.BUILD_REFINERY;
        }

        if (refineryLocations.size() > 1 && designSchoolLocation == null && rc.getTeamSoup() >= DESIGN_SCHOOL.cost) {
            MapLocation loc = makeBuilding(DESIGN_SCHOOL);
            if (loc != null) {
                designSchoolLocation = loc;
                robotBuiltMessages.add(new RobotBuiltMessage(new RobotType[]{RobotType.MINER, RobotType.HQ}, Goal.ALL, designSchoolLocation, DESIGN_SCHOOL));
            }
        }

        if (refineryLocations.size() > 1 && designSchoolLocation != null && fulfillmentLocations.size() < 1 && rc.getTeamSoup() >= FULFILLMENT_CENTER.cost)  {
            MapLocation loc = makeBuilding(FULFILLMENT_CENTER);
            if (loc != null) {
                fulfillmentLocations.add(loc);
                robotBuiltMessages.add(new RobotBuiltMessage(new RobotType[]{RobotType.MINER, RobotType.HQ}, Goal.ALL, loc, FULFILLMENT_CENTER));
            }
        }

        if (!robotBuiltMessages.isEmpty()) {
            boolean success = transactor.submitTransaction(robotBuiltMessages.get(0));
            if (success) {
                System.out.println(robotBuiltMessages.get(0).getRobotType() + " built at " + robotBuiltMessages.get(0).getRobotLocation());
                robotBuiltMessages.remove(0);
            }
        }

        if (soupFoundTransactionNeeded) {
            if (soupLocations.isEmpty()) {
                soupFoundTransactionNeeded = false;
            } else {
                soupFoundTransactionNeeded = !transactor.submitTransaction(new SoupFoundMessage(new RobotType[]{RobotType.MINER}, Goal.ALL, soupLocations.get(0)));
            }
        }
    }

    // Make a building. This ensures we don't put the building in water, that it's not on top of soup, and that it's far enough away
    // from the HQ (just so the building doesn't interfere with building a wall around the HQ.
    private MapLocation makeBuilding(RobotType type) throws GameActionException {
        for (Direction d : Direction.allDirections()) {
            MapLocation buildAt = rc.getLocation().add(d);
            if (rc.canBuildRobot(type, d) && !rc.senseFlooding(buildAt) && rc.senseSoup(buildAt) == 0 && buildAt.distanceSquaredTo(myHQ) >= 9) {
                rc.buildRobot(type, d);
                System.out.println(type + " built at " + buildAt);
                return buildAt;
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
            return Goal.IDLE;
        } else if (rc.canSenseLocation(soupLocations.get(0)) && !rc.isLocationOccupied(soupLocations.get(0)) || !rc.canSenseLocation(soupLocations.get(0))) {
            pathFinder.setGoal(soupLocations.get(0));
        } else {
            pathFinder.setGoal(getOpenTileAdjacent(soupLocations.get(0), soupLocations.get(0).directionTo(rc.getLocation()), Constants.directions, true));
        }

        return Goal.MINE;
    }

    private MapLocation findOrBuildRefinery() throws GameActionException {
        if (refineryLocations.size() == 1) {
            return refineryLocations.get(0);
        }

        MapLocation closest = null;
        int minDist = 0;
        for (MapLocation loc : refineryLocations) {
            if (!loc.equals(myHQ) && (closest == null || rc.getLocation().distanceSquaredTo(loc) < minDist)) {
                minDist = rc.getLocation().distanceSquaredTo(loc);
                closest = loc;
            }
        }

        if (rc.getLocation().distanceSquaredTo(closest) > FAR_THRESHOLD_RADIUS_SQUARED &&
                rc.getTeamSoup() > REFINERY.cost && landscapersBuilt >= INITIAL_ATTACKING_LANDSCAPERS + LANDSCAPERS_FOR_WALL) {
            MapLocation builtAt = makeBuilding(REFINERY);
            if (builtAt != null) {
                robotBuiltMessages.add(new RobotBuiltMessage(new RobotType[]{RobotType.MINER}, Goal.ALL, builtAt, REFINERY));
                return builtAt;
            }
        }

        return closest;
    }

    private boolean farFromAllSharedSoup(MapLocation soupLoc) {
        for (MapLocation soup : sharedSoupLocations) {
            if (soupLoc.distanceSquaredTo(soup) <= FAR_THRESHOLD_RADIUS_SQUARED) {
                return false;
            }
        }

        return true;
    }
}
