package jers;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Transaction;
import jers.Messages.Message;
import jers.Messages.MessageType;
import jers.Messages.RefineryBuiltMessage;

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

    public ArrayList<Message> getBlock(int roundNum) throws GameActionException {
        Transaction[] transactions = rc.getBlock(roundNum);
        return deserializeBlock(transactions, roundNum);
    }

    /**
     * Deserialize the relevant transactions from a block into a Message array.
     *
     * @param transactions The transactions to deserialize.
     * @param fromRound The round on which the block was sent.
     * @return An array of relevant messages from the block.
     */
    private ArrayList<Message> deserializeBlock(Transaction[] transactions, int fromRound) {
        ArrayList<Message> messages = new ArrayList<Message>(transactions.length);

        for (Transaction t : transactions) {
            if (Message.transactionIsForMe(t, fromRound)) {
                if (t.getMessage()[2] == MessageType.REFINERY_BUILT.getId()) {
                    messages.add(new RefineryBuiltMessage(t.getMessage(), 3));
                }
            }
        }

        return messages;
    }
}
