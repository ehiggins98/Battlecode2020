package jers;
import battlecode.common.*;
import jers.Messages.RefineryBuiltMessage;
import jers.Robots.HQ;
import jers.Robots.Miner;
import jers.Robots.Refinery;
import jers.Robots.Robot;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Robot robot;

    static Team team;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        RobotPlayer.team = rc.getTeam();
        try {
            switch(rc.getType()) {
                case HQ:
                    robot = new HQ(rc);
                    break;
                case MINER:
                    robot = new Miner(rc);
                    break;
                case REFINERY:
                    robot = new Refinery(rc);
                    break;
            }

        } catch (Exception e) {
            System.out.println(rc.getType() + " Exception");
            e.printStackTrace();
        }

        while (true) {
            try {
                robot.run(rc.getRoundNum());
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }

            Clock.yield();
        }
    }
}
