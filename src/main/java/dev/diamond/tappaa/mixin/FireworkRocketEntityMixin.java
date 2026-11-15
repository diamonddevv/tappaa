package dev.diamond.tappaa.mixin;

import dev.diamond.tappaa.duel.Duel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends Projectile {
    public FireworkRocketEntityMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Inject(method = "dealExplosionDamage", at = @At("HEAD"), cancellable = true)
    private void tappaa$cancelDuelDeathFireworkDamage_ImAlreadyDeadYouDontNeedToHurtMeMore(ServerLevel level, CallbackInfo ci) {
        if (this.entityTags().contains(Duel.TAG_DEATH_FIREWORK)) ci.cancel();
    }
}
