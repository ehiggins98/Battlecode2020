package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

public class SoupFoundMessage extends Message {

    private RobotType[] recipients;
    private Goal recipientGoal;
    private MapLocation location;

    public SoupFoundMessage(RobotType[] recipients, Goal recipientGoal, MapLocation location) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
        this.location = location;
    }

    public SoupFoundMessage(int[] params, int index) {
        this.location = new MapLocation(params[index], params[index+1]);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SOUP_FOUND;
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
