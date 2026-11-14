package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.TappaaUtil;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Duel {

    public static final AttachmentType<UUID> DUEL_ID_ATTACHMENT = AttachmentRegistry.create(
            Tappaa.id("duel_id"),
            builder -> builder
                    .persistent(UUIDUtil.CODEC)
    );

    private final UUID uuid;
    private final HashMap<ServerPlayer, PlayerDuelState> players;

    public Duel(UUID uuid, Collection<ServerPlayer> players) {
        this.uuid = uuid;
        this.players = new HashMap<>();
        
        players.forEach(player -> {
            this.players.put(player, new PlayerDuelState(player));
            setDuelAttachment(player);
        });
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean involvesPlayer(ServerPlayer player) {
        return players.containsKey(player);
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

    public void withState(ServerPlayer player, Consumer<PlayerDuelState> consumer) {
        consumer.accept(players.get(player));
    }

    public void checkDuelStatus() {

    }

    public void endDuel() {
        this.players.keySet().forEach(this::clearDuelAttachment);
        DuelManager.INSTANCE.removeDuel(this);
    }

    public void onPlayerDied(ServerPlayer player) {
        TappaaUtil.playSoundToPlayer(player, SoundEvents.WARDEN_ATTACK_IMPACT, 1, 0.8f);
        withState(player, state -> state.setEliminated(true));
        checkDuelStatus();
    }

    public void onKilledPlayer(ServerPlayer player) {

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
}
