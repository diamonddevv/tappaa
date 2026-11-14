package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.group.Group;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class DuelManager {

    private final HashMap<UUID, Duel> duels;
    protected DuelManager() {
        this.duels = new HashMap<>();
    }

    public static final DuelManager INSTANCE = new DuelManager();


    public void createDuelFromGroup(Group group) {
        UUID uuid = UUID.randomUUID();
        Duel duel = new Duel(uuid, group.getMembers());
        duels.put(uuid, duel);
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
        ServerLivingEntityEvents.AFTER_DAMAGE.register(DuelManager::entityTookDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register(DuelManager::allowEntityDeath);
    }

    private static void entityTookDamage(LivingEntity livingEntity, DamageSource damageSource, float baseDamageTaken, float damageTaken, boolean blocked) {
    }


    private static boolean allowEntityDeath(LivingEntity livingEntity, DamageSource damageSource, float damageAmount) {
        if (livingEntity.isHolding(item -> item.has(DataComponents.DEATH_PROTECTION))) {
            return true;
        }

        if (livingEntity instanceof ServerPlayer player) {
            Optional<Duel> duel = DuelManager.INSTANCE.getDuel(player);

            if (duel.isPresent()) {

                Entity killerEntity = damageSource.getDirectEntity();
                if (killerEntity instanceof ServerPlayer killerPlayer && duel.get().involvesPlayer(killerPlayer)) {
                    // do something with the player that killed them
                    duel.get().onKilledPlayer(killerPlayer);
                }

                Duel.spawnKillFirework(player);

                player.setGameMode(GameType.SPECTATOR);
                player.setHealth(20.0f);

                duel.get().onPlayerDied(player);

                return false;
            }
        }
        return true;
    }
}
