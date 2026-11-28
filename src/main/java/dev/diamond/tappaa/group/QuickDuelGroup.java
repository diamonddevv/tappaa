package dev.diamond.tappaa.group;

import dev.diamond.tappaa.duel.DuelManager;
import dev.diamond.tappaa.duel.Kit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class QuickDuelGroup extends Group {
    private final Kit kit;

    public QuickDuelGroup(UUID uuid, ServerPlayer owner, Kit kit) {
        super(uuid, owner);
        this.kit = kit;
    }

    @Override
    public void onRespondInvite() {
        if (getRequests().isEmpty()) {
            if (getMemberUuids().size() > 1) {
                DuelManager.INSTANCE.createDuelFromGroup(this, kit, true);
            } else {
                GroupManager.INSTANCE.disbandGroup(this, false);
            }
        }
    }
}
