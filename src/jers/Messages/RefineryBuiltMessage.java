package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

public class RefineryBuiltMessage extends Message {

    private MapLocation builtAt;
    private RobotType[] recipients;
    private Goal recipientGoal;

    public RefineryBuiltMessage(RobotType[] recipients, Goal goal, MapLocation builtAt) {
        this.builtAt = builtAt;
        this.recipients = recipients;
        this.recipientGoal = goal;
    }

    public RefineryBuiltMessage(int[] data, int index) {
        builtAt = new MapLocation(data[index], data[index + 1]);
    }

    public MessageType getMessageType() {
        return MessageType.REFINERY_BUILT;
    }

    public MapLocation getRefineryLocation() {
        return builtAt;
    }

    public int[] getParams() {
        return new int[]{builtAt.x, builtAt.y};
    }

    public RobotType[] getRecipients() {
        return this.recipients;
    }

    public Goal getRecipientGoal() {
        return this.recipientGoal;
    }
}
