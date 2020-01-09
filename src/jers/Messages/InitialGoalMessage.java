package jers.Messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import jers.Goal;

/**
 * Tells a unit to use a certain initial goal.
 * **/
public class InitialGoalMessage extends Message {

    private RobotType[] recipients;
    private Goal recipientGoal;
    private MapLocation initialLocation;
    private int roundCreated;
    private Goal initialGoal;

    /**
     * The initial location of the robot and the round created serve as
     * a unique identifier for it.
     * @param initialLocation The location at which the robot was built.
     * @param roundCreated The round during which the robot was created.
     * @param initialGoal The initial goal to send the robot.
     */
    public InitialGoalMessage(RobotType[] recipients, Goal recipientGoal, MapLocation initialLocation, int roundCreated, Goal initialGoal) {
        this.recipients = recipients;
        this.recipientGoal = recipientGoal;
        this.initialLocation = initialLocation;
        this.roundCreated = roundCreated;
        this.initialGoal = initialGoal;
    }

    /**
     * Deserialize the given params into an initial goal message.
     * @param params The params to deserialize.
     * @param index The index at which to start reading in params.
     */
    public InitialGoalMessage(int[] params, int index) {
        if (params.length < 4 + index) {
            throw new IllegalArgumentException("Must have exactly 4 parameters");
        }

        this.initialLocation = new MapLocation(params[index], params[index + 1]);
        this.roundCreated = params[index + 2];
        this.initialGoal = Goal.fromId(params[index + 3]);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.INITIAL_GOAL;
    }

    @Override
    int[] getParams() {
        int[] params = new int[4];
        params[0] = initialLocation.x;
        params[1] = initialLocation.y;
        params[2] = roundCreated;
        params[3] = initialGoal.getId();

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

    public MapLocation getInitialLocation() {
        return this.initialLocation;
    }

    public int getRoundCreated() {
        return this.roundCreated;
    }

    public Goal getInitialGoal() {
        return this.initialGoal;
    }
}
