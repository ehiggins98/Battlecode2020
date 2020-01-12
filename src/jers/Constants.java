package jers;

import battlecode.common.Direction;

import java.util.Arrays;
import java.util.HashSet;

public class Constants {
    public static int INITIAL_ATTACKING_LANDSCAPERS = 1;
    public static int INITIAL_ATTACKING_DRONES = 4;
    public static int INITIAL_DEFENDING_DRONES = 4;
    public static int DRONE_LANDSCAPER_PAIRS = 4;
    public static int LANDSCAPERS_FOR_WALL = 4;
    public static HashSet<Direction> directions = new HashSet<>(Arrays.asList(Direction.allDirections()));
    public static HashSet<Direction> cardinalDirections = new HashSet<>(Arrays.asList(Direction.cardinalDirections()));
}
