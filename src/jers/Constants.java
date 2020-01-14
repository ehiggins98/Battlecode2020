package jers;

import battlecode.common.Direction;

import java.util.Arrays;
import java.util.HashSet;

public class Constants {
    public static int INITIAL_DEFENDING_DRONES = 4;
    public static int LANDSCAPERS_FOR_WALL = 8;
    // Distance needed to be considered "far" from HQ. We broadcast soup locations
    // if found beyond this distance, and build a refinery if soup is found at least
    // this far from HQ.
    public static int FAR_THRESHOLD_RADIUS_SQUARED = 25;
    public static HashSet<Direction> directions = new HashSet<>(Arrays.asList(Direction.allDirections()));
    public static HashSet<Direction> cardinalDirections = new HashSet<>(Arrays.asList(Direction.cardinalDirections()));
}
