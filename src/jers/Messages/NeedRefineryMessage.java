package jers.Messages;

import battlecode.common.RobotType;
import jers.Goal;

public class NeedRefineryMessage extends Message {

    private RobotType[] recipients;
    private Goal recipientGoal;

    public NeedRefineryMessage() {}

    public NeedRefineryMessage(RobotType[] recipients, Goal recipientGoal) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.REFINERY_NEEDED;
    }

    @Override
    int[] getParams() {
        return new int[0];
    }

    @Override
    RobotType[] getRecipients() {
        return recipients;
    }

    @Override
    Goal getRecipientGoal() {
        return recipientGoal;
    }
}
