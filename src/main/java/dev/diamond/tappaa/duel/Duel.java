package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.TappaaUtil;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Duel {

    public static final AttachmentType<UUID> DUEL_ID_ATTACHMENT = AttachmentRegistry.create(
            Tappaa.id("duel_id"),
            builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .copyOnDeath()
    );

    private final UUID uuid;
    private final HashMap<UUID, PlayerDuelState> players;
    private final int respawnTicks;
    private final MinecraftServer server;

    public Duel(UUID uuid, MinecraftServer server, Collection<ServerPlayer> players) {
        this.uuid = uuid;
        this.server = server;
        this.players = new HashMap<>();

        this.respawnTicks = 5 * 20;
        
        players.forEach(player -> {
            this.players.put(player.getUUID(), new PlayerDuelState(player, 3));
            setDuelAttachment(player);
        });

        onStartDuel();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Collection<ServerPlayer> getPlayers() {
        return this.players.keySet().stream().map(server.getPlayerList()::getPlayer).toList();
    }

    public boolean involvesPlayer(ServerPlayer player) {
        return players.containsKey(player.getUUID());
    }

    public PlayerDuelState getDuelState(ServerPlayer player) {
        return players.get(player.getUUID());
    }

    public void setDuelAttachment(ServerPlayer player) {
        player.setAttached(DUEL_ID_ATTACHMENT, uuid);
    }

    public void clearDuelAttachment(ServerPlayer player) {
        Optional<UUID> uuid = getDuelUuid(player);
        if (uuid.isEmpty() || uuid.get() == getUuid()) {
            player.removeAttached(DUEL_ID_ATTACHMENT);
        }
    }

    public static Optional<UUID> getDuelUuid(ServerPlayer player) {
        if (player.hasAttached(DUEL_ID_ATTACHMENT)) {
            return Optional.ofNullable(player.getAttached(DUEL_ID_ATTACHMENT));
        } else {
            return Optional.empty();
        }
    }

    public boolean testAllPlayerStates(Predicate<PlayerDuelState> state) {
        return players.values().stream().allMatch(state);
    }

    public long countPlayerStates(Predicate<PlayerDuelState> state) {
        return players.values().stream().filter(state).count();
    }

    public void withState(ServerPlayer player, Consumer<PlayerDuelState> consumer) {
        consumer.accept(players.get(player.getUUID()));
    }

    public void forEachMemberOfDuel(Consumer<ServerPlayer> consumer) {
        getPlayers().forEach(consumer);
    }

    public void checkDuelStatus() {
        long playersAlive = countPlayerStates(PlayerDuelState::hasRemainingLives);
        if (playersAlive == 1) {
            endDuel();
        }
    }

    public void endDuel() {

        Component leaderboardMessage = resolveFinalLeaderboardComponent(3);

        forEachMemberOfDuel(player -> {
            clearDuelAttachment(player);
            player.sendSystemMessage(
                    Component.empty()
                            .append("Duel ended.").withStyle(ChatFormatting.YELLOW)
            );

            player.sendSystemMessage(leaderboardMessage);

            PlayerDuelState duelState = getDuelState(player);
            player.setGameMode(duelState.getPreDuelGamemode());
            player.snapTo(duelState.getPreDuelPosition());
        });

        DuelManager.INSTANCE.removeDuel(this);
    }

    public void onStartDuel() {
        forEachMemberOfDuel(player -> {
            player.setGameMode(GameType.SURVIVAL);
        });
    }

    public void onPlayerDied(ServerPlayer player, @Nullable ServerPlayer killer) {
        TappaaUtil.playSoundToPlayer(player, SoundEvents.WARDEN_ATTACK_IMPACT, 1, 0.8f);

        MutableComponent deathMessage = Component.empty().append(TappaaUtil.playerTextComponent(player, ChatFormatting.GOLD));
        if (killer == null) {
            deathMessage.append(Component.literal(" died").withStyle(ChatFormatting.YELLOW));
        } else {
            deathMessage.append(Component.literal(" was killed by ").withStyle(ChatFormatting.YELLOW)).append(TappaaUtil.playerTextComponent(killer, ChatFormatting.GOLD));
        }

        forEachMemberOfDuel(member -> {
            if (member != player) {
                TappaaUtil.playSoundToPlayer(member, SoundEvents.NOTE_BLOCK_COW_BELL.value(), 1, 0.1f);
            }
            member.sendSystemMessage(deathMessage);
        });

        Duel.spawnKillFirework(player);

        // output
        player.setGameMode(GameType.SPECTATOR);
        player.setHealth(20.0f);
        withState(player, state -> {
            state.reduceLives();
            player.sendSystemMessage(Component.literal("Lives remaining: " + state.remainingLives()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            state.setRespawnTimer(this.respawnTicks);
            if (state.hasRemainingLives()) {
                state.setAwaitingRespawn(true);
            }
        });

        checkDuelStatus();
    }

    public void onRespawn(ServerPlayer player) {
        PlayerDuelState duelState = getDuelState(player);

        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(duelState.getPreDuelPosition());
        duelState.setAwaitingRespawn(false);
    }

    public void onKilledPlayer(ServerPlayer killer) {
        withState(killer, PlayerDuelState::incrementKills);
    }

    public static void spawnKillFirework(ServerPlayer player) {
        List<FireworkExplosion> explosions = new ArrayList<>();
        explosions.add(new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of(
                0x800606,
                0xab0909,
                0x9f1212
        ), IntList.of(
                0x540808,
                0xacacac,
                0x000000
        ), true, false));

        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET, 1);
        stack.set(DataComponents.FIREWORKS, new Fireworks(0, explosions));

        FireworkRocketEntity firework = new FireworkRocketEntity(
                player.level(),
                player.getX(), player.getY(), player.getZ(),
                stack
        );
        Projectile.spawnProjectile(firework, player.level(), stack);
    }

    public Component resolveFinalLeaderboardComponent(int results) {
        List<ServerPlayer> players = getPlayers().stream().sorted((player1, player2) -> {

            PlayerDuelState duelState1 = getDuelState(player1);
            PlayerDuelState duelState2 = getDuelState(player2);

            float kdr1 = (duelState1.getKills() + duelState1.getDeaths()) != 0 ? (float) (duelState1.getKills() / (duelState1.getKills() + duelState1.getDeaths())) : 0;
            float kdr2 = (duelState2.getKills() + duelState2.getDeaths()) != 0 ? (float) (duelState2.getKills() / (duelState2.getKills() + duelState2.getDeaths())) : 0;

            if (kdr1 == kdr2) { // if kdr is equal, compare totem pops

                int totems1 = duelState1.getTotemPops();
                int totems2 = duelState2.getTotemPops();

                if (totems1 == totems2) return 0;
                return totems1 > totems2 ? 1 : -1; // sort obverse order - less totems is better
            }
            return kdr1 > kdr2 ? -1 : 1; // sort reverse order - higher kdr is better
        }).toList();

        MutableComponent component = Component.empty();
        component.append(
                Component.literal("=-= RESULTS =-=\n").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
        );

        for (int i = 0; i < results; i++) {
            if (i >= players.size()) break;

            ServerPlayer player = players.get(i);

            component.append(Component.literal((i + 1) + ") ").withStyle(ChatFormatting.YELLOW));
            component.append(TappaaUtil.playerTextComponent(player, 16, ChatFormatting.GOLD));
            component.append(Component.literal(": ").withStyle(ChatFormatting.YELLOW));
            component.append(getKillDeathDisplayComponent(player));
            component.append(Component.literal("\n"));

        }

        return component;
    }

    public Component getKillDeathDisplayComponent(ServerPlayer player) {
        MutableComponent component = Component.empty();

        int kills = getDuelState(player).getKills();
        int deaths = getDuelState(player).getDeaths();
        int totemPops = getDuelState(player).getTotemPops();

        component.append(Component.literal(String.valueOf(kills)).withStyle(ChatFormatting.AQUA));
        component.append(Component.literal(" ⚔ | ").withStyle(ChatFormatting.YELLOW));
        component.append(Component.literal(String.valueOf(deaths)).withStyle(ChatFormatting.AQUA));
        component.append(Component.literal(" ☠ | ").withStyle(ChatFormatting.YELLOW));
        component.append(Component.literal(String.valueOf(totemPops)).withStyle(ChatFormatting.AQUA));
        component.append(Component.literal(" ").withStyle(ChatFormatting.YELLOW));
        component.append(Component.object(new AtlasSprite(
                Identifier.fromNamespaceAndPath("minecraft", "items"),
                Identifier.fromNamespaceAndPath("minecraft", "item/totem_of_undying"))
        ));

        return component;
    }
}
