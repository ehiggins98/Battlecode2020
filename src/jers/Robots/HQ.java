package jers.Robots;

import battlecode.common.*;
import jers.Constants;
import jers.Goal;
import jers.Messages.RobotBuiltMessage;

public class HQ extends Robot {
    private final int INITIAL_MINER_COUNT = 5;
    private final int SECOND_MINER_COUNT = 12;
    private boolean buildMiner = true;
    private boolean locationBroadcast = false;
    private int minersBuilt;
    private RobotBuiltMessage robotBuiltMessage;


    public HQ(RobotController rc) throws GameActionException {
        super(rc);
        minersBuilt = 0;
        goal = Goal.BUILD_INITIAL_MINERS;
        myHQ = rc.getLocation();
    }

    @Override
    public void run(int roundNum) throws GameActionException {
        if (!locationBroadcast && transactor.submitTransaction(new RobotBuiltMessage(RobotType.values(), Goal.ALL, rc.getLocation(), RobotType.HQ))) {
            locationBroadcast = true;
        }

        // Check if drones are nearby and use net gun
        useNetGun();

        switch (goal) {
            case IDLE:
                break;
            case BUILD_INITIAL_MINERS:
                buildInitialMiners();
                break;
            case BUILD_LANDSCAPERS_AND_MINERS:
                buildLandscapersAndMiners(roundNum);
                break;
            default:
                throw new IllegalStateException("Invalid goal for HQ: " + goal);
        }

        writeBlockchain();
    }

    private void buildInitialMiners() throws GameActionException {
        if (rc.isReady() && rc.getTeamSoup() > RobotType.MINER.cost) {
            makeRobot(RobotType.MINER);
            minersBuilt += 1;
        }

        if (minersBuilt >= INITIAL_MINER_COUNT) {
            goal = Goal.BUILD_LANDSCAPERS_AND_MINERS;
        }
    }

    private void buildLandscapersAndMiners(int roundNum) throws GameActionException {
        if (buildMiner) {
            MapLocation builtAt = makeRobot(RobotType.MINER);
            if (builtAt == null) {
                return;
            }

            buildMiner = false;
            minersBuilt += 1;
            robotBuiltMessage = new RobotBuiltMessage(new RobotType[]{RobotType.DESIGN_SCHOOL}, Goal.BUILD_LANDSCAPERS_AND_MINERS, builtAt, RobotType.MINER);

            if (minersBuilt >= Constants.LANDSCAPERS_FOR_WALL + Constants.INITIAL_ATTACKING_LANDSCAPERS + INITIAL_MINER_COUNT) {
                goal = Goal.IDLE;
            }
        } else if (checkRobotBuiltInRound(roundNum - 1, RobotType.LANDSCAPER) != null) {
            buildMiner = true;
        }
    }

    private void writeBlockchain() throws GameActionException {
        if (robotBuiltMessage != null && transactor.submitTransaction(robotBuiltMessage)) {
            robotBuiltMessage = null;
        }
    }

    private void useNetGun() throws GameActionException {
        // Want to find nearest robots
        RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, rc.getTeam().opponent());
        if (robots.length != 0)
        {
            RobotInfo minDistEnemy = robots[0];
            int minDistance = rc.getLocation().distanceSquaredTo(minDistEnemy.location);
            for (RobotInfo robot : robots) {
                if (robot.type != RobotType.DELIVERY_DRONE)
                {
                    continue;
                }

                int newDist = rc.getLocation().distanceSquaredTo(robot.location);
                if (newDist < minDistance) {
                    minDistance = newDist;
                    minDistEnemy = robot;
                }
            }

            if (rc.canShootUnit(minDistEnemy.ID)) {
                rc.shootUnit(minDistEnemy.ID);
            }
        }
    }
}
