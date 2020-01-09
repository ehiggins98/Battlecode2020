package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.InitialGoalMessage;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RobotBuiltMessage;
import jers.Transactor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static jers.Constants.directions;

public abstract class Robot {

    private final int MAX_EXPLORE_DELTA = 10;

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

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        this.goal = Goal.IDLE;
        this.transactor = new Transactor(rc);
        this.createdOnRound = rc.getRoundNum() - 1;
        myHQ = rc.getType() == RobotType.HQ ? rc.getLocation() : checkRobotBuiltInRange(1, 50, RobotType.HQ);
    }

    MapLocation makeRobot(RobotType type) throws GameActionException {
        for (Direction d : directions) {
            if (rc.canBuildRobot(type, d)) {
                rc.buildRobot(type, d);
                return rc.getLocation().add(d);
            }
        }

        return null;
    }

    MapLocation checkRobotBuiltInRound(int inRound, RobotType type) throws GameActionException {
        List<Message> messages = transactor.getBlock(inRound, goal);
        if (messages.size() <= 0) {
            return null;
        }

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg.getMessageType() == MessageType.ROBOT_BUILT && ((RobotBuiltMessage) msg).getRobotType() == type) {
                return ((RobotBuiltMessage) messages.get(i)).getRobotLocation();
            }
        }

        return null;
    }

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

        return null;
    }

    MapLocation getRandomGoal() {
        Random random = new Random();
        int dx = random.nextInt(MAX_EXPLORE_DELTA * 2 + 1) - MAX_EXPLORE_DELTA;
        int dy = random.nextInt(MAX_EXPLORE_DELTA * 2 + 1) - MAX_EXPLORE_DELTA;
        MapLocation currentLoc = rc.getLocation();
        return new MapLocation(currentLoc.x + dx, currentLoc.y + dy);
    }

    MapLocation findOpenAdjacent(MapLocation center, Direction ideal) throws GameActionException {
        Direction rotLeft = ideal;
        Direction rotRight = ideal;

        while (rotLeft != rotRight || rotLeft == ideal) {
            MapLocation rotLeftLoc = center.add(rotLeft);
            MapLocation rotRightLoc = center.add(rotRight);

            if (rc.canSenseLocation(rotLeftLoc) && (!rc.isLocationOccupied(rotLeftLoc) || rc.getLocation().equals(rotLeftLoc)) && !rc.senseFlooding(rotLeftLoc)) {
                return rotLeftLoc;
            } else if (rc.canSenseLocation(rotRightLoc) && (!rc.isLocationOccupied(rotRightLoc) || rc.getLocation().equals(rotLeftLoc)) && !rc.senseFlooding(rotRightLoc)) {
                return rotRightLoc;
            }

            rotLeft = rotLeft.rotateLeft();
            rotRight = rotRight.rotateRight();
        }

        for (Direction d : directions) {
            if (!rc.canSenseLocation(center.add(d))) {
                return center.add(d);
            }
        }

        return null;
    }
}
