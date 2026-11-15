package dev.diamond.tappaa.mixin;

import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.duel.Duel;
import dev.diamond.tappaa.duel.DuelManager;
import dev.diamond.tappaa.duel.PlayerDuelState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {


    @Inject(method = "tick", at = @At("TAIL"))
    private void tappaa$tickOnServerPlayer(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer)(Object)this;
        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(self);
        if (duel.isPresent()) {
            PlayerDuelState duelState = duel.get().getDuelState(self);
            if (duelState == null) return;

            int respawnTicks = duelState.remainingRespawnTicks();


            if (respawnTicks > 0 && duelState.hasRemainingLives() && duelState.isAwaitingRespawn()) {
                if (respawnTicks % 20 == 0) {
                    TappaaUtil.setPlayerActionbarText(self, Component.literal("Respawning in: " + Math.round(respawnTicks / 20f) + " seconds"));
                }

                duelState.decrementRespawnTimer();
            }

            if (respawnTicks <= 0 && duelState.hasRemainingLives() && duelState.isAwaitingRespawn()) {
                duel.get().onRespawn(self);
            }

            if (!duelState.isAwaitingRespawn()) {
                TappaaUtil.setPlayerActionbarText(self, duel.get().getKillDeathDisplayComponent(self));
            }


        }
    }
}
