package jers.Messages;

import battlecode.common.RobotType;
import jers.Goal;

public class UnitPickedUpMessage extends Message {

    private RobotType[] recipients;
    private Goal recipientGoal;
    private int unitId;

    public UnitPickedUpMessage(RobotType[] recipients, Goal recipientGoal, int unitId) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
        this.unitId = unitId;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.UNIT_PICKED_UP;
    }

    @Override
    int[] getParams() {
        return new int[]{this.unitId};
    }

    @Override
    RobotType[] getRecipients() {
        return this.recipients;
    }

    @Override
    Goal getRecipientGoal() {
        return this.recipientGoal;
    }

    public int getUnitId() {
        return unitId;
    }
}
