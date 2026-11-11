package dev.diamond.tappaa.duel;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class DuelManager {

    private final HashMap<UUID, DuelRequest> global_requests;

    protected DuelManager() {
        this.global_requests = new HashMap<>();
    }

    public static final DuelManager INSTANCE = new DuelManager();


    public boolean checkValidRequestExists(ServerPlayer requestee, ServerPlayer target) {
        UUID requesteeUuid = requestee.getUUID();
        if (!this.global_requests.containsKey(requesteeUuid)) {
            return false;
        } else {
            UUID targetUuid = target.getUUID();
            DuelRequest request = this.global_requests.get(requesteeUuid);
            boolean valid = this.checkValid(request);

            return valid && request.getTargetUuids().contains(targetUuid);
        }
    }

    public boolean playerHasActiveRequest(ServerPlayer requestee) {
        if (this.global_requests.containsKey(requestee.getUUID())) {
            return checkValid(this.global_requests.get(requestee.getUUID()));
        }
        return false;
    }

    public boolean checkValid(DuelRequest request) {
        if (request.cancelled() || request.accepted()) {
            this.global_requests.remove(request.getRequesteeUuid());
            return false;
        }
        return true;
    }

    public DuelRequest requestDuel(ServerPlayer requestee, Collection<ServerPlayer> targets) {
        DuelRequest request = new DuelRequest(requestee, targets);
        this.global_requests.put(request.getRequesteeUuid(), request);
        return request;
    }

    public DuelRequest getRequestedDuel(ServerPlayer requestee) {
        return this.global_requests.get(requestee.getUUID());
    }


    public void acceptDuelRequest(DuelRequest request) {
        request.markAccepted();
    }

}
