package dev.diamond.tappaa.mixin;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {
    @Accessor("lifetime")
    int tappaa$getFireworkLifetime();

    @Accessor("lifetime")
    void tappaa$setFireworkLifetime(int ticks);
}
