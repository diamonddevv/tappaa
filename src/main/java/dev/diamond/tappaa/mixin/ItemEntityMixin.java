package dev.diamond.tappaa.mixin;

import dev.diamond.tappaa.duel.Duel;
import dev.diamond.tappaa.duel.DuelManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    @Shadow
    public abstract @Nullable Entity getOwner();

    public ItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void tappaa$preventItemPickup(Player player, CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            if (getOwner() instanceof ServerPlayer owner) {
                Optional<Duel> ownerDuel = DuelManager.INSTANCE.getDuel(owner);
                if (ownerDuel.isPresent()) {
                    if (owner != player) ci.cancel();
                }
            }
        }
    }
}
