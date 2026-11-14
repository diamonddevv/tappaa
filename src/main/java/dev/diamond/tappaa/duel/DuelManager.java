package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.group.Group;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            for (Duel duel : DuelManager.INSTANCE.duels.values()) {
                if (duel.involvesPlayer(player)) { // FIXME THIS SUCKS SO BAD WE CANT BE LOOPING OVER EVERY DUEL. KEEP A REFERENCE ON THE PLAYER DATA LIKE GROUPS DO

                    Entity killerEntity = damageSource.getDirectEntity();
                    if (killerEntity instanceof ServerPlayer killerPlayer && duel.involvesPlayer(killerPlayer)) {
                        // do something with the player that killed them
                    }

                    Duel.spawnKillFirework(player);

                    player.setGameMode(GameType.SPECTATOR);
                    player.setHealth(20.0f);

                    return false;
                }
            }
        }
        return true;
    }
}
