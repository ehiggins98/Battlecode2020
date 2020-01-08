package jers.Robots;

import battlecode.common.*;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RefineryBuiltMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.ArrayList;

import static jers.Constants.directions;

enum Goal {IDLE, MINE, REFINE, EXPLORE;}

public class Miner extends Robot {
    private final int MIN_SOUP_FOR_TRANSACTION = 2 * RobotType.MINER.soupLimit;

    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation refineryLocation = null;
    boolean refineryTransactionNeeded = false;
    private Goal goal = Goal.IDLE;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        pathFinder = new PathFinder(rc);
        transactor = new Transactor(rc);
        MapLocation localSoup = findLocalSoup();
        if (localSoup != null) {
            if (rc.senseSoup(localSoup) > MIN_SOUP_FOR_TRANSACTION) {
                //Send a transaction to tell other miners to come here
            }

            pathFinder.setGoal(localSoup);
            System.out.println("Goal set.");
            goal = Goal.MINE;
        }
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (refineryLocation == null && roundNum > 59) {
            refineryLocation = findRefineryLocation(roundNum);
        }
        if (refineryTransactionNeeded) {
            refineryTransactionNeeded = !transactor.submitTransaction(new RefineryBuiltMessage(refineryLocation));
        }

        if (refineryLocation == null) {
            MapLocation loc = makeRefinery();
            if (loc != null) {
                refineryLocation = loc;
                refineryTransactionNeeded = true;
            }
        }
        if (!pathFinder.isFinished()) {
            System.out.println("At " + rc.getLocation() + " with " + rc.senseSoup(rc.getLocation()) + " soup");
            boolean success = pathFinder.move();
            System.out.println("Moved to " + rc.getLocation());
            System.out.println(success);

            if (!success || goal == Goal.IDLE) {
                MapLocation farSoup = findFarSoup();
                if (farSoup != null) {
                    if (rc.senseSoup(farSoup) > MIN_SOUP_FOR_TRANSACTION) {
                        //Send a transaction to tell other miners to come here
                    }
                    pathFinder.setGoal(farSoup);
                    goal = Goal.MINE;
                }
            }
            if (goal == Goal.MINE) {
                MapLocation goal_loc = pathFinder.getGoal();
                if ((rc.senseSoup(goal_loc) == 0) ||
                        (rc.isLocationOccupied(goal_loc) && !rc.getLocation().equals(goal_loc))) {
                    MapLocation loc = findLocalSoup();
                    if (loc != null) {
                        pathFinder.setGoal(loc);
                    }
                }
            }
        }
        else if (rc.isReady()) {
            if (goal == Goal.MINE) {
                if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                    if (rc.senseSoup(pathFinder.getGoal()) == 0) {
                        MapLocation loc = findLocalSoup();
                        if (loc != null) {
                            pathFinder.setGoal(loc);
                        }
                        else {
                            goal = Goal.IDLE;
                        }
                    }
                    else if (rc.senseSoup(rc.getLocation()) > 0) {
                        rc.mineSoup(Direction.CENTER);
                    }
                }
                else if (refineryLocation != null) {
                    pathFinder.setGoal(refineryLocation.add(Direction.SOUTH));
                    goal = Goal.REFINE;
                }
            }

            if (goal == Goal.REFINE) {
                // Dumb solution but since we're always below refinery at finish we just deposit upwards
                rc.depositSoup(Direction.NORTH, rc.getSoupCarrying());
                goal = Goal.MINE;
            }
        }
    }

    private MapLocation makeRefinery() throws GameActionException {
        for (Direction d : directions) {
            MapLocation buildAt = rc.getLocation().add(d);
            if (rc.canBuildRobot(RobotType.REFINERY, d) && !rc.senseFlooding(buildAt) && rc.senseSoup(buildAt) == 0) {
                rc.buildRobot(RobotType.REFINERY, d);
                return buildAt;
            }
        }

        return null;
    }

    private MapLocation findRefineryLocation(int roundNum) throws GameActionException {
        // Searching a round costs 100 bytecode, so we'll limit to 50 rounds to
        // be safe.
        for (int round = 59; round < Math.min(109, roundNum); round++) {
            ArrayList<Message> messages = transactor.getBlock(round);
            for (Message m : messages) {
                if (m.getMessageType() == MessageType.REFINERY_BUILT) {
                    return ((RefineryBuiltMessage) m).getRefineryLocation();
                }
            }
        }

        return null;
    }

    /**
     * Find first instance of soup on the outskirts of the range
     * Use this after moving to determine if new soup has been discovered
     * If new soup is found, send transaction detailing location of soup
     * Locations are manually input so we don't have to check if within sensor range, just if on the map;
     * @return
     * @throws GameActionException
     */
    private MapLocation findFarSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int[][] coords = new int[][] {
                new int[]{loc.x-4, loc.y-4},
                new int[]{loc.x-3, loc.y-5},
                new int[]{loc.x-2, loc.y-5},
                new int[]{loc.x-1, loc.y-5},
                new int[]{loc.x, loc.y-5},
                new int[]{loc.x+1, loc.y-5},
                new int[]{loc.x+2, loc.y-5},
                new int[]{loc.x+3, loc.y-5},
                new int[]{loc.x+4, loc.y-4},
                new int[]{loc.x+5, loc.y-3},
                new int[]{loc.x+5, loc.y-2},
                new int[]{loc.x+5, loc.y-1},
                new int[]{loc.x+5, loc.y},
                new int[]{loc.x+5, loc.y+1},
                new int[]{loc.x+5, loc.y+2},
                new int[]{loc.x+5, loc.y+3},
                new int[]{loc.x+4, loc.y+4},
                new int[]{loc.x-3, loc.y+5},
                new int[]{loc.x-2, loc.y+5},
                new int[]{loc.x-1, loc.y+5},
                new int[]{loc.x, loc.y+5},
                new int[]{loc.x+1, loc.y+5},
                new int[]{loc.x+2, loc.y+5},
                new int[]{loc.x+3, loc.y+5},
                new int[]{loc.x-4, loc.y+4},
                new int[]{loc.x-5, loc.y-3},
                new int[]{loc.x-5, loc.y-2},
                new int[]{loc.x-5, loc.y-1},
                new int[]{loc.x-5, loc.y},
                new int[]{loc.x-5, loc.y+1},
                new int[]{loc.x-5, loc.y+2},
                new int[]{loc.x-5, loc.y+3}};

        System.out.println("My coords are " + loc.x + " " + loc.y);
        for (int[] coord: coords) {
            MapLocation newLoc = new MapLocation(coord[0], coord[1]);
            try {
                if (rc.onTheMap(newLoc) && rc.senseSoup(newLoc) > 0) {
                    return newLoc;
                }
            }
            catch (GameActionException e) {
                System.out.println("Not valid coords: " + coord[0] + " " + coord[1]);
            }
        }

        return null;
    }

    /**
     * Find first instance of soup in the range
     * For MapLocations in x-4, y-4 to x+4, y+4 check if soup is present
     * If so, send transaction detailing location of soup
     * @return
     * @throws GameActionException
     */
    private MapLocation findLocalSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
        for (int x = loc.x-4; x <= loc.x+4; x++) {
            for (int y = loc.y - 4; y <= loc.y+4; y++) {
                if (x == loc.x-4 && y == loc.y-4 || x == loc.x-4 && y == loc.y+4 || x == loc.x+4 && y == loc.y-4 || x == loc.x+4 && y == loc.y+4) {
                    continue;
                }
                MapLocation new_loc = new MapLocation(x, y);
                if (rc.onTheMap(new_loc) && rc.senseSoup(new_loc) > 0 && !rc.isLocationOccupied(new_loc)) {
                    return new_loc;
                }
            }
        }

        return null;
    }
}
