package dev.diamond.tappaa.duel;

import net.minecraft.server.level.ServerPlayer;

public class PlayerDuelState {
    private final ServerPlayer player;

    private int kills;
    private int lives;

    public PlayerDuelState(ServerPlayer player, int initialLives) {
        this.player = player;
        this.kills = 0;
        this.lives = initialLives;
    }


    public void reduceLives() {
        this.lives -= 1;
    }

    public boolean isEliminated() {
        return this.lives <= 0;
    }

    public boolean isAlive() {
        return this.lives > 0;
    }
}
