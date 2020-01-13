package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

public class HqFoundMessage extends Message {

    private RobotType[] recipients;
    private Goal recipientGoal;
    private MapLocation location;

    public HqFoundMessage(RobotType[] recipients, Goal recipientGoal, MapLocation location) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
        this.location = location;
    }

    public HqFoundMessage(int[] data, int index) {
        location = new MapLocation(data[index], data[index+1]);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.HQ_FOUND;
    }

    @Override
    int[] getParams() {
        return new int[]{location.x, location.y};
    }

    @Override
    RobotType[] getRecipients() {
        return recipients;
    }

    @Override
    Goal getRecipientGoal() {
        return recipientGoal;
    }

    public MapLocation getLocation() {
        return location;
    }
}
