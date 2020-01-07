package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RefineryBuiltMessage;
import jers.PathFinder;
import jers.Transactor;

import java.util.ArrayList;
import java.util.Map;

import static jers.Constants.directions;

enum Goal {IDLE, MINING, REFINING;}

public class Miner extends Robot {
    private PathFinder pathFinder;
    private Transactor transactor;
    private MapLocation refineryLocation = null;
    boolean refineryTransactionNeeded = false;
    Goal goal = Goal.IDLE;

    public Miner(RobotController rc) {
        super(rc);

        pathFinder = new PathFinder(rc);
        pathFinder.setGoal(new MapLocation(0, 0));
        transactor = new Transactor(rc);

        try {
            MapLocation localSoup = findLocalSoup();
            if (localSoup != null) {
                if (rc.senseSoup(localSoup) > 2 * RobotType.MINER.soupLimit) {
                    //Send a transaction to tell other miners to come here
                }
                pathFinder.setGoal(localSoup);
                goal = Goal.MINING;
            }
        }
        catch (GameActionException e) {

        }

    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (refineryLocation == null && roundNum > 70) {
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
            pathFinder.move();
            if (goal == Goal.IDLE) {
                MapLocation farSoup = findFarSoup();
                if (farSoup != null) {
                    if (rc.senseSoup(farSoup) > 2 * RobotType.MINER.soupLimit) {
                        //Send a transaction to tell other miners to come here
                    }
                    pathFinder.setGoal(farSoup);
                    goal = Goal.MINING;
                }
            }
        }
        else {
            if (rc.isReady()) {
                if (goal == Goal.MINING) {
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
                        else {
                            rc.mineSoup(Direction.CENTER);
                        }
                    }
                    else {
                        pathFinder.setGoal(refineryLocation.add(Direction.SOUTH));
                        goal = Goal.REFINING;
                    }
                }

                if (goal == Goal.REFINING) {
                    // Dumb solution but since we're always below refinery at finish we just deposit upwards
                    rc.depositSoup(Direction.NORTH, 100);
                    goal = Goal.MINING;
                }

            }
        }
    }

    private MapLocation makeRefinery() throws GameActionException {
        for (Direction d : directions) {
            if (rc.canBuildRobot(RobotType.REFINERY, d)) {
                MapLocation builtAt = rc.getLocation().add(d);
                rc.buildRobot(RobotType.REFINERY, d);
                return builtAt;
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

    // Find first instance of soup on the outskirts of the range
    // Use this after moving to determine if new soup has been discovered
    // If new soup is found, send transaction detailing location of soup
    // Locations are manually input so we don't have to check if within sensor range, just if on the map;
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
            MapLocation new_loc = new MapLocation(coord[0], coord[1]);
            try {
                if (rc.onTheMap(new_loc) && rc.senseSoup(new_loc) > 0) {
                    return new_loc;
                }
            }
            catch (GameActionException e) {
                System.out.println("Not valid coords: " + coord[0] + " " + coord[1]);
            }
        }

        return null;
    }

    // Find first instance of soup on the inskirts of the range
    // For MapLocations in x-4, y-4 to x+4, y+4 check if soup is present
    // If so, send transaction detailing location of soup
    private MapLocation findLocalSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
        for (int x = loc.x-4; x <= loc.x+4; x++) {
            for (int y = loc.y - 4; y <= loc.y+4; y++) {
                if (x == loc.x-4 && y == loc.y-4 || x == loc.x-4 && y == loc.y+4 || x == loc.x+4 && y == loc.y-4 || x == loc.x+4 && y == loc.y+4) {
                    continue;
                }
                MapLocation new_loc = new MapLocation(x, y);
                if (rc.onTheMap(new_loc) && rc.senseSoup(new_loc) > 0) {
                    return new_loc;
                }
            }
        }

        return null;
    }
}
