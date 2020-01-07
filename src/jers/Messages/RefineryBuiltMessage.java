package jers.Messages;

import battlecode.common.MapLocation;

public class RefineryBuiltMessage extends Message {

    private MapLocation builtAt;

    public RefineryBuiltMessage(MapLocation builtAt) {
        this.builtAt = builtAt;
    }

    public RefineryBuiltMessage(int[] data, int index) {
        builtAt = new MapLocation(data[index], data[index + 1]);
    }

    public MessageType getMessageType() {
        return MessageType.REFINERY_BUILT;
    }

    public MapLocation getRefineryLocation() {
        return builtAt;
    }

    public int[] getParams() {
        return new int[]{builtAt.x, builtAt.y};
    }
}
