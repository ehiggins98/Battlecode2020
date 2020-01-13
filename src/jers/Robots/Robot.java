package jers.Robots;

import battlecode.common.*;
import jers.Goal;
import jers.Messages.*;
import jers.Transactor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public abstract class Robot {

    /**
     * Run one turn for the robot.
     * @throws GameActionException
     */
    public abstract void run(int roundNum) throws GameActionException;

    RobotController rc;
    Goal goal;
    Transactor transactor;
    int createdOnRound;
    MapLocation myHQ;

    public Robot(RobotController rc) {
        this.rc = rc;
        this.goal = Goal.IDLE;
        this.transactor = new Transactor(rc);
        this.createdOnRound = rc.getRoundNum() - 1;
    }

    /**
     * Builds a new instance of the given robot type on one available tile.
     * @param type The type of robot to build.
     * @return The location at which the robot was built.
     * @throws GameActionException
     */
    MapLocation makeRobot(RobotType type) throws GameActionException {
        for (Direction d : Direction.allDirections()) {
            if (rc.canBuildRobot(type, d)) {
                rc.buildRobot(type, d);
                return rc.getLocation().add(d);
            }
        }

        return null;
    }

    /**
     * Check if a robot of the given type was built in the given round.
     * @param inRound The round to check.
     * @param type The type of robot to check.
     * @return The location of the robot if one was built, or null otherwise.
     * @throws GameActionException
     */
    MapLocation checkRobotBuiltInRound(int inRound, RobotType type) throws GameActionException {
        return checkRobotBuiltInRange(inRound, inRound + 1, type);
    }

    /**
     * Check if a robot was built in the given range of rounds.
     * @param startRound The first round to check, inclusive.
     * @param endRound The last round to check, exclusive.
     * @param type The type of robot to check.
     * @return The location of the first robot of the given type built in the range, or null if there was none.
     * @throws GameActionException
     */
    MapLocation checkRobotBuiltInRange(int startRound, int endRound, RobotType type) throws GameActionException {
        for (int round = startRound; round < endRound; round++) {
            ArrayList<Message> messages = transactor.getBlock(round, this.goal);
            for (Message m : messages) {
                if (m.getMessageType() == MessageType.ROBOT_BUILT && ((RobotBuiltMessage) m).getRobotType() == type) {
                    return ((RobotBuiltMessage) m).getRobotLocation();
                }
            }
        }

        return null;
    }

    /**
     * Get the initial goal for the robot created at the given location in the given round.
     * @param initialLocation The initial location of the robot for which to retrieve the initial goal.
     * @param currentRound The current round.
     * @return The initial goal if a message was sent with one, or IDLE otherwise.
     * @throws GameActionException
     */
    Goal checkInitialGoal(MapLocation initialLocation, int currentRound) throws GameActionException {
        for (int round = createdOnRound; round < Math.min(10 + createdOnRound, currentRound); round++) {
            ArrayList<Message> messages = transactor.getBlock(round, this.goal);

            for (Message m : messages) {
                if (m.getMessageType() != MessageType.INITIAL_GOAL) {
                    continue;
                }

                InitialGoalMessage initialGoalMessage = (InitialGoalMessage) m;
                if (initialGoalMessage.getInitialLocation().equals(initialLocation) && initialGoalMessage.getRoundCreated() == createdOnRound) {
                    return initialGoalMessage.getInitialGoal();
                }
            }
        }

        return Goal.IDLE;
    }

    ArrayList<MapLocation> checkSoupFoundInRound(int round) throws GameActionException {
        return checkSoupFoundInRange(round, round + 1);
    }

    ArrayList<MapLocation> checkSoupFoundInRange(int startRound, int endRound) throws GameActionException {
        ArrayList<MapLocation> results = new ArrayList<>();
        for (int round = startRound; round < endRound; round++) {
            ArrayList<Message> messages = transactor.getBlock(round, goal);
            for (Message m : messages) {
                if (m.getMessageType() != MessageType.SOUP_FOUND) {
                    continue;
                }

                results.add(((SoupFoundMessage) m).getLocation());
            }
        }

        return results;
    }

    /**
     * Get an open tile adjacent to the given tile, starting from the ideal direction and proceeding outward through the
     * given valid directions.
     * @param center The tile for which to find an adjacent location.
     * @param ideal The ideal direction.
     * @param valid Valid directions to consider.
     * @return The open location closest to the ideal direction, or null if there is no open valid direction.
     * @throws GameActionException
     */
    MapLocation getOpenTileAdjacent(MapLocation center, Direction ideal, HashSet<Direction> valid, boolean avoidHQ) throws GameActionException {
        Direction rotLeft = ideal;
        Direction rotRight = ideal;

        while (rotLeft != rotRight || rotLeft == ideal) {
            MapLocation rotLeftLoc = center.add(rotLeft);
            MapLocation rotRightLoc = center.add(rotRight);

            if (valid.contains(rotLeft) && rc.canSenseLocation(rotLeftLoc) &&
                    (!rc.isLocationOccupied(rotLeftLoc) || rc.getLocation().equals(rotLeftLoc)) &&
                    !rc.senseFlooding(rotLeftLoc) && (!avoidHQ || !rotLeftLoc.isAdjacentTo(myHQ))) {
                return rotLeftLoc;
            } else if (valid.contains(rotRight) && rc.canSenseLocation(rotRightLoc) &&
                    (!rc.isLocationOccupied(rotRightLoc) || rc.getLocation().equals(rotLeftLoc)) &&
                    !rc.senseFlooding(rotRightLoc) && (!avoidHQ || !rotRightLoc.isAdjacentTo(myHQ))) {
                return rotRightLoc;
            }

            rotLeft = rotLeft.rotateLeft();
            rotRight = rotRight.rotateRight();
        }

        for (Direction d : valid) {
            if (!rc.canSenseLocation(center.add(d))) {
                return center.add(d);
            }
        }

        return null;
    }

     class ClosestLocComparator implements Comparator<MapLocation> {

        private MapLocation center;

        ClosestLocComparator(MapLocation center) {
            this.center = center;
        }


        @Override
        public int compare(MapLocation o1, MapLocation o2) {
            return Integer.compare(o1.distanceSquaredTo(center), o2.distanceSquaredTo(center));
        }
    }
}
