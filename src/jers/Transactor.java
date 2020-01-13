package jers;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import jers.Messages.*;

import java.util.ArrayList;

public class Transactor {
    private RobotController rc;

    public Transactor(RobotController rc) {
        this.rc = rc;
    }

    /**
     * Submit a transaction
     * @return Whether we have enough soup to submit the transaction.
     * @throws GameActionException Have to write this, but it'll never be thrown
     */
    public boolean submitTransaction(Message message) throws GameActionException {
        int[] data = message.serialize(rc.getRoundNum());
        // Paying 3 soup per transaction is totally arbitrary.
        if (rc.canSubmitTransaction(data, 3)) {
            rc.submitTransaction(data, 3);
            return true;
        }

        return false;
    }

    /**
     * Read the block from the given turn, filter the transactions to those meant for our team, the current robot type,
     * and the given goal, and return a list of them.
     * @param roundNum The turn for which to read transactions.
     * @param currentGoal The goal on which to filter transactions.
     * @return A list of messages sent on the given turn, meant for the current robot type and goal.
     * @throws GameActionException
     */
    public ArrayList<Message> getBlock(int roundNum, Goal currentGoal) throws GameActionException {
        Transaction[] transactions = rc.getBlock(roundNum);
        return deserializeBlock(transactions, roundNum, currentGoal);
    }

    /**
     * Deserialize the relevant transactions from a block into a Message array.
     *
     * @param transactions The transactions to deserialize.
     * @param fromRound The round on which the block was sent.
     * @return An array of relevant messages from the block.
     */
    private ArrayList<Message> deserializeBlock(Transaction[] transactions, int fromRound, Goal currentGoal) {
        ArrayList<Message> messages = new ArrayList<Message>(transactions.length);

        for (Transaction t : transactions) {
            if (Message.isForMe(t.getMessage(), rc.getType(), currentGoal, fromRound)) {
                int[] msg = t.getMessage();
                if (msg[2] == MessageType.ROBOT_BUILT.id) {
                    messages.add(new RobotBuiltMessage(msg, 3));
                } else if (msg[2] == MessageType.INITIAL_GOAL.id) {
                    messages.add(new InitialGoalMessage(msg, 3));
                } else if (msg[2] == MessageType.SOUP_FOUND.id) {
                    messages.add(new SoupFoundMessage(msg, 3));
                } else if (msg[2] == MessageType.WATER_FOUND.id) {
                    messages.add(new WaterFoundMessage(msg, 3));
                }
            }
        }

        return messages;
    }
}
