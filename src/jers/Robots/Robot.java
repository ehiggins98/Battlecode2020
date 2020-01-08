package jers.Robots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Robot {
    /**
     * Run one turn for the robot.
     * @throws GameActionException
     */
    public abstract void run(int roundNum) throws GameActionException;

    RobotController rc;

    public Robot(RobotController rc) {
        this.rc = rc;
    }
}
