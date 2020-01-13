package jers.Messages;

import battlecode.common.RobotType;
import jers.Goal;

/** Tells that a unit has completed their request
 *
 */
public class RequestCompletedMessage extends Message {
    private RobotType[] recipients;
    private Goal recipientGoal;

    /**
     * We only care about updating our respective unit
     * @param recipients The robot types we want to change
     * @param recipientGoal What goal we want our recipients to have
     */
    public RequestCompletedMessage(RobotType[] recipients, Goal recipientGoal) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
    }

    /**
     * Deserialize the given params into an completed request message.
     * @param params The params to deserialize.
     * @param index The index at which to start reading in params.
     */
    public RequestCompletedMessage(int[] params, int index) {
        if (params.length < index) {
            throw new IllegalArgumentException("Must have 0 parameters");
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_COMPLETED;
    }

    @Override
    int[] getParams() {
        int[] params = new int[0];
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
}
