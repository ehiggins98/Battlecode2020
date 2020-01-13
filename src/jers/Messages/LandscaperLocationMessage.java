package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

public class LandscaperLocationMessage extends Message {
    private RobotType[] recipients;
    private Goal recipientGoal;
    private MapLocation location;

    public LandscaperLocationMessage(RobotType[] recipients, Goal recipientGoal, MapLocation location) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
        this.location = location;
    }

    public LandscaperLocationMessage(int[] params, int index) {
        this.location = new MapLocation(params[index], params[index+1]);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LANDSCAPER_LOCATED;
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
