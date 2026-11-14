package dev.diamond.tappaa.duel;

import net.minecraft.server.level.ServerPlayer;

public class PlayerDuelState {
    private final ServerPlayer player;
    private boolean eliminated;

    public PlayerDuelState(ServerPlayer player) {
        this.player = player;
        this.eliminated = false;
    }


    public void setEliminated(boolean state) {
        this.eliminated = state;
    }
}
