package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.*;
import jers.PathFinder;

import java.util.ArrayList;

public class Landscaper extends Robot {
    // Map is rotationally, horizontally, or vertically symmetric, so we don't know for sure where the HQ is.
    private MapLocation[] theirHQPossibilities;
    private int hqTry;
    private MapLocation theirHQ;
    private PathFinder pathFinder;
    private Direction depositDirection = Direction.CENTER;
    private HqFoundMessage hqFoundMessage;
    private int startupLastRoundChecked;
    private Goal initialGoal;

    public Landscaper(RobotController rc) throws GameActionException {
        super(rc);
        myHQ = checkRobotBuiltInRange(1, 20, RobotType.HQ);
        hqTry = 0;
        pathFinder = new PathFinder(rc);
        goal = Goal.STARTUP;
    }

    /**
     * Some landscapers build a wall around the HQ, while others find the enemy HQ and attempt to bury it.
     * These attacking landscapers will dig through walls built by the enemy.
     * @param roundNum The current round number.
     * @throws GameActionException
     */
    @Override
    public void run(int roundNum) throws GameActionException {
        if (goal == Goal.STARTUP) {
            startUp(roundNum);
            if (goal == Goal.STARTUP) {
                return;
            }
        }

        Goal lastGoal = null;
        readBlockchain(roundNum);
        // Without the while loop we waste turns changing goals
        while (rc.isReady() && goal != null && goal != Goal.IDLE && lastGoal != goal) {
            lastGoal = goal;
            switch (goal) {
                case IDLE:
                case GET_INITIAL_GOAL:
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
                    case HQ_FOUND:
                        theirHQ = ((HqFoundMessage) m).getLocation();
                        break;
                    case INITIAL_GOAL:
                        InitialGoalMessage initialGoalMessage = (InitialGoalMessage) m;
                        if (initialGoalMessage.getRoundCreated() == createdOnRound &&
                                initialGoalMessage.getInitialLocation().equals(rc.getLocation())) {
                            initialGoal = initialGoalMessage.getInitialGoal();
                            System.out.println(initialGoal);
                        }
                        break;
                }
            }
        }
    }

    // Try to bury the enemy HQ.
    private void attackEnemyHQ() throws GameActionException {
        Direction hqDir = rc.getLocation().directionTo(theirHQ);
        if (rc.canDepositDirt(hqDir)) {
            rc.depositDirt(hqDir);
        } else if (rc.canDigDirt(Direction.CENTER)) {
            rc.digDirt(Direction.CENTER);
        }
    }

    // For defensive landscapers, go to our HQ. Since we have 4 landscapers building the wall, they stand at the
    // cardinal directions and build 2 tiles of the wall.
    private void goToMyHQ() throws GameActionException {
        if (pathFinder.getGoal() == null || pathFinder.isFailed()) {
            pathFinder.setGoal(getOpenTileAdjacent(myHQ, myHQ.directionTo(rc.getLocation()).opposite(), Constants.directions, false));
        } else if (pathFinder.isFinished()) {
            goal = Goal.BUILD_HQ_WALL;
        }

        if (rc.isReady()) {
            pathFinder.move(false, false, myHQ);
        }
    }

    // Build the wall. Each landscaper handles 2 tiles.
    private void buildHQWall() throws GameActionException {
        if (rc.canDepositDirt(Direction.CENTER) && rc.getDirtCarrying() > 0) {
            rc.depositDirt(depositDirection);
        } else {
            Direction digDirection = getDigDirection();
            if (digDirection != null && rc.canDigDirt(digDirection)) {
                rc.digDirt(digDirection);
            }
        }
    }

    // Find the direction in which to dig dirt while building the HQ wall. This will avoid destroying the wall
    // and won't dig other landscapers into a ditch.
    private Direction getDigDirection() throws GameActionException {
        if (rc.canDigDirt(rc.getLocation().directionTo(myHQ))) {
            return rc.getLocation().directionTo(myHQ);
        }
        Direction perpendicular = myHQ.directionTo(rc.getLocation());
        Direction[] possibilities = new Direction[]{perpendicular, perpendicular.rotateLeft(), perpendicular.rotateLeft()};

        for (Direction d : possibilities) {
            RobotInfo occupier = rc.senseRobotAtLocation(rc.getLocation().add(d));
            if (rc.canDigDirt(d) && (occupier == null || occupier.getType() != RobotType.LANDSCAPER)) {
                return d;
            }
        }

        return null;
    }

    private void readBlockchain(int roundNum) throws GameActionException {
        ArrayList<Message> messages = transactor.getBlock(roundNum - 1, goal);
        for (Message m : messages) {
            switch (m.getMessageType()) {
                case HQ_FOUND:
                    theirHQ = ((HqFoundMessage) m).getLocation();
                    break;
                case INITIAL_GOAL:
                    System.out.println("Got initial goal message 2");
                    InitialGoalMessage initialGoalMessage = (InitialGoalMessage) m;
                    if (initialGoalMessage.getRoundCreated() == createdOnRound &&
                            initialGoalMessage.getInitialLocation().equals(rc.getLocation())) {
                        initialGoal = initialGoalMessage.getInitialGoal();
                        goal = initialGoal;
                    }
                    break;
            }
        }
    }

    private void writeBlockchain() throws GameActionException {
        if (hqFoundMessage != null) {
            transactor.submitTransaction(hqFoundMessage);
            hqFoundMessage = null;
        }
    }
}
