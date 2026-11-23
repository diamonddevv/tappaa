package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.mixin.FireworkRocketEntityAccessor;
import dev.diamond.tappaa.serial.DuelStats;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Duel {

    public static final AttachmentType<UUID> DUEL_ID_ATTACHMENT = AttachmentRegistry.create(
            Tappaa.id("duel_id"),
            builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .copyOnDeath()
    );
    public static final String TAG_DEATH_FIREWORK = Tappaa.id("duel_death_firework").toString();

    public static final int PREGAME_TICKS = 5 * 20;

    private final UUID uuid;
    private final HashMap<UUID, PlayerDuelState> players;
    private final KitManager kitManager;
    private int pregameTimeTicks;
    private final int respawnTicks;
    private final MinecraftServer server;
    private final boolean useKit;
    private int timerTicks;
    private final Consumer<Duel> onEnd;

    public Duel(UUID uuid, MinecraftServer server, KitManager kit, boolean useKit, Collection<ServerPlayer> players, Consumer<Duel> onEnd) {
        this.uuid = uuid;
        this.server = server;
        this.kitManager = kit;
        this.useKit = useKit;
        this.players = new HashMap<>();

        this.respawnTicks = 5 * 20;
        this.pregameTimeTicks = PREGAME_TICKS;
        this.timerTicks = 0;

        this.onEnd = onEnd;
        
        players.forEach(player -> {
            this.players.put(player.getUUID(), new PlayerDuelState(this, player, 3));
            setDuelAttachment(player);
        });

        onPregameStarts();
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
            endDuel(false);
        }
    }

    public boolean isInPregame() {
        return this.pregameTimeTicks > 0;
    }

    public void tick() {
        this.timerTicks++;

        if (this.isInPregame()) {
            this.pregameTimeTicks--;

            tickPregame();

            if (pregameTimeTicks <= 0) {
                onPregameEnds();
            }
        }
    }

    public void endDuel(boolean endedEarly) {

        Component leaderboardMessage = resolveFinalLeaderboardComponent(3);

        MutableComponent duelEndedMessage = Component.empty()
                .append("Duel ended. ").withStyle(ChatFormatting.YELLOW);

        String timestamp = TappaaUtil.ticksToMinsSecs(this.timerTicks);
        duelEndedMessage.append(
                Component.literal("[" + timestamp + "]\n").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
        );


        if (!endedEarly) {
            Optional<ServerPlayer> winner = getPlayers().stream().min(new DuelLeaderboardComparator(this));
            if (winner.isPresent()) {
                DuelStats.PlayerDuelStats winnerStats = DuelStats.getDuelStats(server).getPlayerStatsOrCreate(winner.get());
                winnerStats.wins += 1;
                DuelStats.getDuelStats(server).putPlayerStats(winner.get(), winnerStats);
            }
        }


        forEachMemberOfDuel(player -> {

            withState(player, state -> {
                DuelStats.PlayerDuelStats stats = DuelStats.getDuelStats(server).getPlayerStatsOrCreate(player);
                stats.duelsPlayed += 1;
                stats.kills += state.getKills();
                stats.deaths += state.getDeaths();
                stats.totemPops += state.getTotemPops();
                DuelStats.getDuelStats(server).putPlayerStats(player, stats);
            });

            clearDuelAttachment(player);
            player.sendSystemMessage(duelEndedMessage);
            player.sendSystemMessage(leaderboardMessage);
            restorePlayerToPreDuel(player);
        });

        this.onEnd.accept(this);

        DuelManager.INSTANCE.removeDuel(this);
    }

    public void eliminatePlayer(ServerPlayer player) {
        if (this.involvesPlayer(player)) {
            Duel.spawnKillFirework(player);
            withState(player, state -> {
                state.setLives(0);
                state.setRespawnTimer(this.respawnTicks);
                state.setAwaitingRespawn(false);
                restorePlayerToPreDuel(player);
            });
            checkDuelStatus();
        }
    }

    public void restorePlayerToPreDuel(ServerPlayer player) {
        withState(player, state -> {
            player.setGameMode(state.getPreDuelGamemode());
            resetHealthAndHunger(player);

            KitManager.clearPlayerOfKitItems(player);
            List<ItemStack> stacks = kitManager.getCopyOfItems(player);
            KitManager.restorePlayerInventory(player, state.getPreDuelItems());
            stacks.forEach(player.getInventory()::add);

            teleport(player, state.getPreDuelPosition());
        });
    }

    public void onPregameStarts() {
        this.pregameTimeTicks = PREGAME_TICKS;

        forEachMemberOfDuel(player -> {
            player.setGameMode(GameType.SURVIVAL);
            player.removeAllEffects();
            resetHealthAndHunger(player);
            if (useKit) {
                kitManager.copyToPlayer(player);
            }
            player.sendSystemMessage(Component.literal("Starting duel!").withStyle(ChatFormatting.YELLOW));
        });
    }

    public void onPregameEnds() {
        forEachMemberOfDuel(player -> {
            withState(player, state -> {
                resetHealthAndHunger(player);
                teleport(player, state.getPreDuelPosition());
            });
        });
    }

    public void tickPregame() {
        forEachMemberOfDuel(player -> {
            if (this.pregameTimeTicks % 20 == 0) {
                TappaaUtil.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BIT.value(), 1f, pregameTimeTicks <= 0 ? 1f : 0.5f);
            }
            TappaaUtil.setPlayerActionbarText(
                    player,
                    Component.literal(
                            String.format("Starting in: %.1f seconds", this.pregameTimeTicks / 20f)
                    )
            );
        });
    }

    public void onPlayerDied(ServerPlayer player, @Nullable ServerPlayer killer) {
        TappaaUtil.playSoundToPlayer(player, SoundEvents.WARDEN_ATTACK_IMPACT, 1, 0.8f);

        MutableComponent deathMessage = Component.empty();

        deathMessage.append(Component.literal("[" + TappaaUtil.ticksToMinsSecs(this.timerTicks) + "] ").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        deathMessage.append(TappaaUtil.playerTextComponent(player, ChatFormatting.GOLD));
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
        resetHealthAndHunger(player); // this needs to happen WHEN we die, not after, or the "you died!" screen still shows
        withState(player, state -> {
            state.reduceLives();
            player.sendSystemMessage(Component.literal("Died in [" + TappaaUtil.ticksToMinsSecs(this.timerTicks - state.getLastDeathTime()) + "] | Lives remaining: " + state.remainingLives()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            state.setRespawnTimer(this.respawnTicks);
            if (state.hasRemainingLives()) {
                state.setAwaitingRespawn(true);
            }
            state.setLastDeathTime(this.timerTicks);
        });

        checkDuelStatus();
    }

    public void onRespawn(ServerPlayer player) {
        PlayerDuelState duelState = getDuelState(player);

        player.setGameMode(GameType.SURVIVAL);
        player.removeAllEffects();
        teleport(player, duelState.getPreDuelPosition());

        KitManager.clearPlayerOfKitItems(player); // remove items that were part of the kit..
        List<ItemStack> stacks = kitManager.getCopyOfItems(player); // copy all the items the player has left..
        if (useKit) {
            kitManager.copyToPlayer(player); // copy all the items that were in the kit, including empty slots, to the player
        } else {
            // restore to pre-duel inventory as if it were the kit
            KitManager.restorePlayerInventory(player, duelState.getPreDuelItems());
        }
        stacks.forEach(player.getInventory()::add); // give back the remaining items, which are items that they picked up during the duel.

        duelState.setAwaitingRespawn(false);
    }

    public void resetHealthAndHunger(ServerPlayer player) {
        player.setHealth(20.0f);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5);
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
        firework.addTag(TAG_DEATH_FIREWORK); // so we can differentiate this from other fireworks and apply some custom behavior in mixins.
        ((FireworkRocketEntityAccessor) firework).tappaa$setFireworkLifetime(0);
        Projectile.spawnProjectile(firework, player.level(), stack);

    }

    public static void teleport(ServerPlayer player, Vec3 pos) {
        player.teleportTo(pos.x, pos.y, pos.z);
    }

    public Component resolveFinalLeaderboardComponent(int results) {
        List<ServerPlayer> players = getPlayers().stream().sorted(new DuelLeaderboardComparator(this)).toList();

        MutableComponent component = Component.empty();

        component.append("\n");

        for (int i = 0; i < results; i++) {
            if (i >= players.size()) break;

            ServerPlayer player = players.get(i);

            component.append(Component.literal((i + 1) + ") ").withStyle(ChatFormatting.YELLOW));
            component.append(TappaaUtil.playerTextComponent(player, 16, ChatFormatting.GOLD));
            component.append(Component.literal(": ").withStyle(ChatFormatting.YELLOW));
            component.append(getStatsDisplayComponent(player, false));
            component.append(Component.literal("\n"));

        }

        return component;
    }

    public Component getStatsDisplayComponent(ServerPlayer player, boolean showLives) {
        MutableComponent component = Component.empty();

        Component seperator = Component.literal(" | ").withStyle(ChatFormatting.YELLOW);

        int kills = getDuelState(player).getKills();
        int deaths = getDuelState(player).getDeaths();
        int totemPops = getDuelState(player).getTotemPops();

        MutableComponent killsComponent = Component.empty();
        killsComponent.append(Component.literal(String.valueOf(kills)).withStyle(ChatFormatting.AQUA));
        killsComponent.append(Component.literal(" ⚔").withStyle(ChatFormatting.YELLOW));
        killsComponent.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(
                Component.literal("Killed other players " + kills + " times")
        )));
        component.append(killsComponent);


        component.append(seperator);

        MutableComponent deathsComponent = Component.empty();
        deathsComponent.append(Component.literal(String.valueOf(deaths)).withStyle(ChatFormatting.AQUA));
        deathsComponent.append(Component.literal(" ☠").withStyle(ChatFormatting.YELLOW));
        deathsComponent.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(
                Component.literal("Died " + deaths + " times")
        )));
        component.append(deathsComponent);

        component.append(seperator);

        MutableComponent totemPopsComponent = Component.empty();
        totemPopsComponent.append(Component.literal(String.valueOf(totemPops)).withStyle(ChatFormatting.AQUA));
        totemPopsComponent.append(Component.literal(" ").withStyle(ChatFormatting.YELLOW));
        totemPopsComponent.append(Component.object(new AtlasSprite(
                Identifier.fromNamespaceAndPath("minecraft", "items"),
                Identifier.fromNamespaceAndPath("minecraft", "item/totem_of_undying"))
        ));
        totemPopsComponent.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(
                Component.literal("Popped other players totems " + totemPops + " times")
        )));
        component.append(totemPopsComponent);

        if (showLives) {
            int lives = getDuelState(player).remainingLives();
            component.append(seperator);
            component.append(Component.literal(String.valueOf(lives)).withStyle(ChatFormatting.AQUA));
            component.append(Component.literal(" ❤").withStyle(ChatFormatting.YELLOW));
        }

        return component;
    }


    private record DuelLeaderboardComparator(Duel duel) implements Comparator<ServerPlayer> {

        @Override
            public int compare(ServerPlayer player1, ServerPlayer player2) {

                PlayerDuelState duelState1 = duel.getDuelState(player1);
                PlayerDuelState duelState2 = duel.getDuelState(player2);

                float kdr1 = (duelState1.getKills() + duelState1.getDeaths()) != 0 ? (float) (duelState1.getKills() / (duelState1.getKills() + duelState1.getDeaths())) : 0;
                float kdr2 = (duelState2.getKills() + duelState2.getDeaths()) != 0 ? (float) (duelState2.getKills() / (duelState2.getKills() + duelState2.getDeaths())) : 0;

                if (kdr1 == kdr2) { // if kdr is equal, compare totem pops

                    int totems1 = duelState1.getTotemPops();
                    int totems2 = duelState2.getTotemPops();

                    if (totems1 == totems2) return 0;
                    return totems1 > totems2 ? -1 : 1; // sort reverse order - more totems is better (we count how many this player has popped other players')
                }
                return kdr1 > kdr2 ? -1 : 1; // sort reverse order - higher kdr is better
            }
        }
}
