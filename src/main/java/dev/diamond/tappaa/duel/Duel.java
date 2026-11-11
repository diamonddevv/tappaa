package dev.diamond.tappaa.duel;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

public class Duel {
    private final UUID uuid;
    private final Collection<ServerPlayer> players;

    public Duel(UUID uuid, Collection<ServerPlayer> players) {
        this.uuid = uuid;
        this.players = players;
    }


    public UUID getUuid() {
        return uuid;
    }
}
