package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

/**
 * Tells types of units with specific goals to change their goal.
 * **/
public class ChangeGoalMessage extends Message {
    private RobotType[] recipients;
    private Goal recipientGoal;
    private Goal changeGoalTo;

    /**
     * We only care about units with a particular goal
     * @param recipients The robot types we want to change
     * @param recipientGoal What goal we want our recipients to have
     * @param changeGoalTo The goal we want to change our units to
     */
    public ChangeGoalMessage(RobotType[] recipients, Goal recipientGoal, Goal changeGoalTo) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
        this.changeGoalTo = changeGoalTo;
    }

    /**
     * Deserialize the given params into an change goal message.
     * @param params The params to deserialize.
     * @param index The index at which to start reading in params.
     */
    public ChangeGoalMessage(int[] params, int index) {
        if (params.length < 1 + index) {
            throw new IllegalArgumentException("Must have exactly 1 parameter");
        }

        this.changeGoalTo = Goal.fromId(params[index]);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CHANGE_GOAL;
    }

    @Override
    int[] getParams() {
        int[] params = new int[1];
        params[0] = changeGoalTo.getId();

        return params;
    }

    @Override
    RobotType[] getRecipients() {
        return this.recipients;
    }

    @Override
    Goal getRecipientGoal() {
        return this.recipientGoal;
    }

    public Goal getChangeGoalTo() {
        return this.changeGoalTo;
    }
}
