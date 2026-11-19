package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class DuelManager {

    private final HashMap<UUID, Duel> duels;
    protected DuelManager() {
        this.duels = new HashMap<>();
    }

    public static final DuelManager INSTANCE = new DuelManager();


    public Optional<Duel> createDuelFromGroup(Group group, boolean disbandGroupAfter, boolean useKit) {
        if (!group.hasValidActiveDuel()) {
            UUID uuid = UUID.randomUUID();
            Duel duel = new Duel(uuid, group.getServer(), group.getKitManager(), useKit, group.getMembers(), disbandGroupAfter ? d -> {
                GroupManager.INSTANCE.disbandGroup(group, true);
            } : d -> {});
            group.setDuelUuid(uuid);
            duels.put(uuid, duel);
            return Optional.of(duel);
        }
        return Optional.empty();
    }

    public Optional<Duel> findDuel(UUID uuid) {
        return Optional.ofNullable(this.duels.get(uuid));
    }

    public Optional<Duel> getDuel(ServerPlayer player) {
        return Duel.getDuelUuid(player).flatMap(this::findDuel);
    }

    public void removeDuel(Duel duel) {
        this.duels.remove(duel.getUuid());
    }

    public static void registerDuelingEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(DuelManager::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(DuelManager::serverStopping);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(DuelManager::canEntityTakeDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register(DuelManager::allowEntityDeath);
        ServerPlayerEvents.JOIN.register(DuelManager::playerJoinsServer);
        ServerPlayerEvents.LEAVE.register(DuelManager::playerLeavesServer);
        ServerTickEvents.START_SERVER_TICK.register(DuelManager::serverTick);
        BlockEvents.USE_ITEM_ON.register(DuelManager::useBlockWithItem);
        BlockEvents.USE_WITHOUT_ITEM.register(DuelManager::useBlockWithoutItem);
    }

    private static void serverStarted(MinecraftServer minecraftServer) {
        // immediately add the default kit
        KitManager.saveDefaultKit(minecraftServer);
    }

    private static void serverStopping(MinecraftServer minecraftServer) {

        for (Duel duel : DuelManager.INSTANCE.duels.values()) {
            duel.forEachMemberOfDuel(duel::restorePlayerToPreDuel);
        }

    }

    private static boolean canEntityTakeDamage(LivingEntity livingEntity, DamageSource damageSource, float damage) {
        if (livingEntity instanceof ServerPlayer player) {
            Optional<Duel> duel = DuelManager.INSTANCE.getDuel(player);
            if (duel.isPresent()) {

                if (duel.get().isInPregame()) return false;

                Entity aggressor = damageSource.getEntity();
                return !(aggressor instanceof ServerPlayer aggressorPlayer) ||
                        duel.get().involvesPlayer(aggressorPlayer);
            }
        }
        return true;
    }


    private static boolean allowEntityDeath(LivingEntity livingEntity, DamageSource damageSource, float damageAmount) {

        if (livingEntity instanceof ServerPlayer player) {
            Optional<Duel> duel = DuelManager.INSTANCE.getDuel(player);

            if (duel.isPresent()) {

                Entity killerEntity = damageSource.getDirectEntity();
                ServerPlayer killer = null;

                if (player.isHolding(item -> item.has(DataComponents.DEATH_PROTECTION))) {
                    if (killerEntity instanceof ServerPlayer killerPlayer && duel.get().involvesPlayer(killerPlayer)) {
                        duel.get().getDuelState(killerPlayer).incrementTotemPops();
                    }
                    return true;
                }


                if (killerEntity instanceof ServerPlayer killerPlayer && duel.get().involvesPlayer(killerPlayer)) {
                    // do something with the player that killed them
                    killer = killerPlayer;
                    duel.get().onKilledPlayer(killerPlayer);
                }





                duel.get().onPlayerDied(player, killer);

                return false;
            }
        }
        return true;
    }

    private static void playerJoinsServer(ServerPlayer player) {
        Optional<UUID> uuid = Duel.getDuelUuid(player);
        uuid.ifPresent(value -> DuelManager.INSTANCE.findDuel(value).ifPresentOrElse(duel -> {
            duel.eliminatePlayer(player);
        }, () -> {
            player.removeAttached(Duel.DUEL_ID_ATTACHMENT);
        }));
    }

    private static void playerLeavesServer(ServerPlayer player) {
        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(player);
        if (duel.isPresent()) {
            duel.get().forEachMemberOfDuel(member -> {
                member.sendSystemMessage(
                        Component.empty()
                                .append(TappaaUtil.playerTextComponent(player, ChatFormatting.GOLD, ChatFormatting.ITALIC))
                                .append(Component.literal(" was automatically eliminated from the duel.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                );
            });
            duel.get().eliminatePlayer(player);
        }
    }

    private static void serverTick(MinecraftServer server) {
        for (Duel duel : DuelManager.INSTANCE.duels.values()) {
            duel.tick();
        }
    }

    private static @Nullable InteractionResult useBlockWithItem(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (DuelManager.INSTANCE.getDuel(serverPlayer).isPresent()) {
                if (blockState.getBlock() instanceof BaseEntityBlock) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return null;
    }

    private static @Nullable InteractionResult useBlockWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (DuelManager.INSTANCE.getDuel(serverPlayer).isPresent()) {
                if (blockState.getBlock() instanceof BaseEntityBlock) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return null;
    }

}
