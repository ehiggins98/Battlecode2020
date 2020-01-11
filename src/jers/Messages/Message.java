package jers.Messages;

import battlecode.common.GameConstants;
import battlecode.common.RobotType;
import jers.Goal;

import java.util.Arrays;

public abstract class Message {
    /**
     * Gets the ID of this message type.
     * @return
     */
    public abstract MessageType getMessageType();

    /**
     * Gets the parameters associated with this message type.
     * Note: This must be of length at most 4. We use 2 integers
     * to sign the message, 1 for the message type, which leaves
     * 4 remaining.
     * @return
     */
    abstract int[] getParams();

    /**
     * Get the robot types that should pay attention to this message.
     * @return The robot types that should pay attention to this message.
     */
    abstract RobotType[] getRecipients();

    /**
     * Robots with the given goal should pay attention to the message.
     * @return The goal for which robots should pay attention to the message.
     */
    abstract Goal getRecipientGoal();

    private static final int SECRET_VALUE = 81076;
    private static final int MODULUS = 31;
    private static final int RECIPIENT_SPEC_LENGTH = 16;

    /**
     * Serializes the message for sending with 94875, roundNum % 31, and the message type ID.
     * The first 2 are completely arbitrary and serve only to decrease the risk of collision
     * with the opponent's messages.
     *
     * roundNum % 31 is included just so our opponent can't notice that all our messages start with the
     * same number and decide to send us false messages.
     * @param roundNum The round number the message was sent in.
     * @return The serialized message.
     * @throws IllegalArgumentException If the message has more than 4 parameters.
     */
    public int[] serialize(int roundNum) throws IllegalArgumentException {
        if (getParams().length > GameConstants.MAX_BLOCKCHAIN_TRANSACTION_LENGTH - 3) {
            throw new IllegalArgumentException("Too many parameters.");
        }

        int[] data = new int[3 + getParams().length];
        data[0] = makeSigningValue(roundNum);
        data[1] = makeTargetValue(getRecipients(), getRecipientGoal());
        data[2] = getMessageType().id;

        for (int i = 0; i < getParams().length; i++) {
            data[i + 3] = getParams()[i];
        }

        return data;
    }

    /**
     * Checks if the given transaction was sent by us.
     * @param msg The message to check.
     * @param sentOnRound The round the transaction was sent on.
     * @return A value indicating whether the current transaction was sent by us.
     */
    public static boolean isForMe(int[] msg, RobotType type, Goal goal, int sentOnRound) {
        if (msg.length < 3) {
            return false;
        }

        if (!checkSigningValue(sentOnRound, msg[0])) {
            return false;
        }

        return checkTargetValue(msg[1], type, goal);
    }

    // Make the signing value. This has as the least-significant 17 bits the SECRET_VALUE, and as the
    // most-significant 15 bits the round % MODULUS repeated 3 times.
    private static int makeSigningValue(int roundNum) {
        int hash = roundNum % MODULUS;
        return (((((hash << 5) + hash) << 5) + hash) << 17) + SECRET_VALUE;
    }

    // Check the received signing value.
    private static boolean checkSigningValue(int roundNum, int signingValue) {
        int expected = makeSigningValue(roundNum);
        return signingValue == expected;
    }

    // This uses the goal ID as the most-significant 16 bits, and a one-hot encoding of the intended recipients
    // as the least-significant 15 bits.
    private int makeTargetValue(RobotType[] recipients, Goal recipientGoals) {
        int recipientSpec = getRecipientEncoding(recipients);
        int goalSpec = recipientGoals.getId();
        return (goalSpec << RECIPIENT_SPEC_LENGTH) + recipientSpec;
    }

    // Check whether the received target value is targeting the given robot type and goal.
    private static boolean checkTargetValue(int value, RobotType type, Goal goal) {
        int robotTypeEncoding = Arrays.asList(RobotType.values()).indexOf(type);
        if ((value >> robotTypeEncoding) % 2 == 0) {
            return false;
        }

        int goalSpec = value >> RECIPIENT_SPEC_LENGTH;
        return goalSpec == goal.getId() || goalSpec == Goal.ALL.getId();
    }

    // Get the encoding for the given recipient list.
    private int getRecipientEncoding(RobotType[] types) {
        int value = 0;

        // This is a one-hot encoding, so a robot can check if it should pay
        // attention to a given message in O(1) time.
        for (RobotType type : types) {
            value |= 1 << Arrays.asList(RobotType.values()).indexOf(type);
        }

        return value;
    }
}
