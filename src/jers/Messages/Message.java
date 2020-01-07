package jers.Messages;

import battlecode.common.GameConstants;
import battlecode.common.Transaction;

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

    private static final int FIRST_VALUE = 94875;
    private static final int SECOND_VALUE_MODULUS = 31;

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
        data[0] = FIRST_VALUE;
        data[1] = roundNum % SECOND_VALUE_MODULUS;
        data[2] = getMessageType().getId();

        for (int i = 0; i < getParams().length; i++) {
            data[i+3] = getParams()[i];
        }

        return data;
    }

    /**
     * Checks if the given transaction was sent by us.
     * @param transaction The transaction to check.
     * @param sentOnRound The round the transaction was sent on.
     * @return A value indicating whether the current transaction was sent by us.
     */
    public static boolean transactionIsForMe(Transaction transaction, int sentOnRound) {
        int[] msg = transaction.getMessage();
        return msg.length >= 3 && msg[0] == FIRST_VALUE && msg[1] == sentOnRound % SECOND_VALUE_MODULUS;
    }
}
