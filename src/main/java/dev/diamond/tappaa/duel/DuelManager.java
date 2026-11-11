package dev.diamond.tappaa.duel;

import net.minecraft.server.level.ServerPlayer;

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
            if (request.cancelled())
            return targetUuid == request.getTargetUuid();
        }
    }

    public boolean playerHasActiveRequest(ServerPlayer requestee) {
        return this.global_requests.containsKey(requestee.getUUID());
    }

    public DuelRequest requestDuel(ServerPlayer requestee, ServerPlayer target) {
        DuelRequest request = new DuelRequest(requestee, target);
        this.global_requests.put(request.getRequesteeUuid(), request);
        return request;
    }

    public DuelRequest getRequestedDuel(ServerPlayer requestee) {
        return this.global_requests.get(requestee.getUUID());
    }

}
