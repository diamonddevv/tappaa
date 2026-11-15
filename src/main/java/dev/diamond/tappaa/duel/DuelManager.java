package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.group.Group;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class DuelManager {

    private final HashMap<UUID, Duel> duels;
    protected DuelManager() {
        this.duels = new HashMap<>();
    }

    public static final DuelManager INSTANCE = new DuelManager();


    public Optional<Duel> createDuelFromGroup(Group group) {
        if (!group.hasValidActiveDuel()) {
            UUID uuid = UUID.randomUUID();
            Duel duel = new Duel(uuid, group.getServer(), group.getMembers());
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
        ServerLivingEntityEvents.AFTER_DAMAGE.register(DuelManager::entityTookDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register(DuelManager::allowEntityDeath);
    }

    private static void entityTookDamage(LivingEntity livingEntity, DamageSource damageSource, float baseDamageTaken, float damageTaken, boolean blocked) {
    }


    private static boolean allowEntityDeath(LivingEntity livingEntity, DamageSource damageSource, float damageAmount) {

        if (livingEntity instanceof ServerPlayer player) {
            Optional<Duel> duel = DuelManager.INSTANCE.getDuel(player);

            if (duel.isPresent()) {

                if (player.isHolding(item -> item.has(DataComponents.DEATH_PROTECTION))) {
                    duel.get().getDuelState(player).incrementTotemPops();
                    return true;
                }

                Entity killerEntity = damageSource.getDirectEntity();
                ServerPlayer killer = null;
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
}
