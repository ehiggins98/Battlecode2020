package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

public class WaterFoundMessage extends Message {

    private RobotType[] recipients;
    private Goal recipientGoal;
    private MapLocation location;

    public WaterFoundMessage(RobotType[] recipients, Goal recipientGoal, MapLocation location) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
        this.location = location;
    }

    public WaterFoundMessage(int[] data, int index) {
        this.location = new MapLocation(data[index], data[index + 1]);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.WATER_FOUND;
    }

    @Override
    int[] getParams() {
        return new int[]{this.location.x, this.location.y};
    }

    @Override
    RobotType[] getRecipients() {
        return this.recipients;
    }

    @Override
    Goal getRecipientGoal() {
        return this.recipientGoal;
    }

    public MapLocation getLocation() {
        return location;
    }
}
