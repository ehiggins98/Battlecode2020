package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

import java.util.Arrays;

/**
 * Signals when a robot has been built, and at what location
 * it was built.
 */
public class RobotBuiltMessage extends Message {

    private MapLocation builtAt;
    private RobotType[] recipients;
    private Goal recipientGoal;
    private RobotType type;

    public RobotBuiltMessage(RobotType[] recipients, Goal goal, MapLocation builtAt, RobotType type) {
        this.builtAt = builtAt;
        this.recipients = recipients;
        this.recipientGoal = goal;
        this.type = type;
    }

    public RobotBuiltMessage(int[] data, int index) {
        builtAt = new MapLocation(data[index], data[index + 1]);
        type = Arrays.asList(RobotType.values()).get(data[index + 2]);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ROBOT_BUILT;
    }

    @Override
    public int[] getParams() {
        return new int[]{this.builtAt.x, this.builtAt.y, Arrays.asList(RobotType.values()).indexOf(type)};
    }

    @Override
    public RobotType[] getRecipients() {
        return this.recipients;
    }

    @Override
    public Goal getRecipientGoal() {
        return this.recipientGoal;
    }

    public MapLocation getRobotLocation() {
        return this.builtAt;
    }

    public RobotType getRobotType() { return this.type; }
}
