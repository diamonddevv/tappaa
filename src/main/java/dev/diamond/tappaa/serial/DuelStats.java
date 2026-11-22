package dev.diamond.tappaa.serial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.diamond.tappaa.Tappaa;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class DuelStats extends SavedData {
    /*
     * There's a good chance ill replace this with a proper database.
     */

    private static final Codec<DuelStats> CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC, PlayerDuelStats.CODEC
    ).xmap(
            DuelStats::new,
            DuelStats::getMap
    );

    private static final SavedDataType<DuelStats> TYPE = new SavedDataType<>(
            Tappaa.id("duel_stats"),
            DuelStats::new,
            CODEC, null
    );

    private final Map<UUID, PlayerDuelStats> map;

    private DuelStats() {
        this.map = new HashMap<>();
    }
    private DuelStats(Map<UUID, PlayerDuelStats> map) {
        this.map = new HashMap<>(map);
    }

    private Map<UUID, PlayerDuelStats> getMap() {
        return map;
    }


    public PlayerDuelStats getPlayerStatsOrCreate(ServerPlayer player) {
        return Optional.ofNullable(this.map.get(player.getUUID())).orElse(new PlayerDuelStats(0, 0, 0, 0, 0));
    }

    public void putPlayerStats(ServerPlayer player, PlayerDuelStats stats) {
        this.map.put(player.getUUID(), stats);
        this.setDirty();
    }


    public static DuelStats getDuelStats(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }


    public static final class PlayerDuelStats {
            private static final Codec<PlayerDuelStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("duels_played").forGetter(PlayerDuelStats::duelsPlayed),
                    Codec.INT.fieldOf("wins").forGetter(PlayerDuelStats::wins),
                    Codec.INT.fieldOf("kills").forGetter(PlayerDuelStats::kills),
                    Codec.INT.fieldOf("deaths").forGetter(PlayerDuelStats::deaths),
                    Codec.INT.fieldOf("totem_pops").forGetter(PlayerDuelStats::totemPops)
            ).apply(instance, PlayerDuelStats::new));

        public int duelsPlayed;
        public int wins;
        public int kills;
        public int deaths;
        public int totemPops;

        public PlayerDuelStats(int duelsPlayed, int wins, int kills, int deaths, int totemPops) {
            this.duelsPlayed = duelsPlayed;
            this.wins = wins;
            this.kills = kills;
            this.deaths = deaths;
            this.totemPops = totemPops;
        }

        private int duelsPlayed() {
            return duelsPlayed;
        }

        private int wins() {
            return wins;
        }

        private int kills() {
            return kills;
        }

        private int deaths() {
            return deaths;
        }

        private int totemPops() {
            return totemPops;
        }

    }

}
