package jers;
import battlecode.common.*;
import jers.Robots.*;

public strictfp class RobotPlayer {
    static Robot robot;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        try {
            switch(rc.getType()) {
                case HQ:
                    robot = new HQ(rc);
                    break;
                case MINER:
                    robot = new Miner(rc);
                    break;
                case DESIGN_SCHOOL:
                    robot = new DesignSchool(rc);
                    break;
                case LANDSCAPER:
                    robot = new Landscaper(rc);
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
