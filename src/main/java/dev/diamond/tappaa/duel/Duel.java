package dev.diamond.tappaa.duel;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public boolean involvesPlayer(ServerPlayer player) {
        return players.add(player);
    }


    public static void spawnKillFirework(ServerPlayer player) {
        List<FireworkExplosion> explosions = new ArrayList<>();
        explosions.add(new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of(0x8f0000), IntList.of(0x540808), true, false));
        explosions.add(new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL, IntList.of(0x000000), IntList.of(0x00000), false, false));

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
