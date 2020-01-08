package jers;

import battlecode.common.Direction;
import battlecode.common.RobotType;

import java.util.Arrays;
import java.util.List;

public class Constants {
    public static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
            Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};

    public static List<RobotType> robotTypes = Arrays.asList(RobotType.MINER, RobotType.REFINERY, RobotType.HQ, RobotType.COW,
            RobotType.DELIVERY_DRONE, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.LANDSCAPER,
            RobotType.NET_GUN, RobotType.VAPORATOR);
}
