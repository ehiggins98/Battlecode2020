package jers.Robots;

import battlecode.common.*;

public class NetGun extends Robot {

    public NetGun(RobotController rc) {
        super(rc);
    }

    @Override
    public void run(int roundNum) throws GameActionException {
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
