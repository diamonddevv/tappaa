package dev.diamond.tappaa.duel;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class PlayerDuelState {
    private final Duel duel;
    private final ServerPlayer player;

    private final GameType preDuelGamemode;
    private final Vec3 preDuelPosition;
    private final NonNullList<ItemStack> preDuelItems;
    private int lastDeathTime;

    private int kills;
    private int deaths;
    private int totemPops;

    private int respawnTimer;
    private boolean awaitingRespawn;
    private int lives;

    public PlayerDuelState(Duel duel, ServerPlayer player, int initialLives) {
        this.duel = duel;
        this.player = player;

        this.preDuelGamemode = player.gameMode();
        this.preDuelPosition = player.position();
        this.preDuelItems = KitManager.savePlayerInventory(player);

        this.kills = 0;
        this.deaths = 0;
        this.totemPops = 0;

        this.respawnTimer = 0;
        this.lastDeathTime = 0;
        this.awaitingRespawn = false;
        this.lives = initialLives;
    }

    public void incrementKills() {
        this.kills++;
    }

    public void incrementTotemPops() {
        this.totemPops++;
    }

    public int getKills() {
        return this.kills;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public int getTotemPops() {
        return this.totemPops;
    }

    public void reduceLives() {
        this.lives -= 1;
        this.deaths += 1;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public boolean hasRemainingLives() {
        return this.lives > 0;
    }

    public int remainingLives() {
        return this.lives;
    }

    public void setRespawnTimer(int ticks) {
        this.respawnTimer = ticks;
    }

    public int remainingRespawnTicks() {
        return this.respawnTimer;
    }

    public void decrementRespawnTimer() {
        this.respawnTimer--;
    }

    public int getLastDeathTime() {
        return this.lastDeathTime;
    }

    public void setLastDeathTime(int ticks) {
        this.lastDeathTime = ticks;
    }

    public boolean isAwaitingRespawn() {
        return awaitingRespawn;
    }

    public void setAwaitingRespawn(boolean state) {
        awaitingRespawn = state;
    }

    public GameType getPreDuelGamemode() {
        return preDuelGamemode;
    }

    public Vec3 getPreDuelPosition() {
        return preDuelPosition;
    }

    public NonNullList<ItemStack> getPreDuelItems() {
        return preDuelItems;
    }
}
