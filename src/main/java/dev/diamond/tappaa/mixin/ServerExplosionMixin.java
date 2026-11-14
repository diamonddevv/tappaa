package dev.diamond.tappaa.mixin;

import dev.diamond.tappaa.duel.DuelManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ServerExplosion;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {

    @Shadow
    public abstract @Nullable LivingEntity getIndirectSourceEntity();

//    @Inject(method = "interactsWithBlocks", at = @At("HEAD"), cancellable = true)
//    private void tappaa$overrideExplosionBlockInteraction(CallbackInfoReturnable<Boolean> cir) {
//        if (getIndirectSourceEntity() instanceof ServerPlayer player) {
//            if (DuelManager.INSTANCE.getDuel(player).isPresent()) {
//                cir.setReturnValue(false);
//            }
//        }
//
//        // ill come back to this later
//    }

}
