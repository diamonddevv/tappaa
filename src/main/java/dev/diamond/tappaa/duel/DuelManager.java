package dev.diamond.tappaa.duel;

import com.mojang.serialization.Codec;
import dev.diamond.tappaa.Tappaa;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.serialization.v1.MoreCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

import java.util.*;

public class DuelManager {

    private static final UUID UUID_EMPTY = new UUID(0, 0);
    public static final AttachmentType<UUID> DUEL_ID_ATTACHMENT = AttachmentRegistry.create(
            Tappaa.id("duel_id"),
            builder -> builder
                    .initializer(() -> UUID_EMPTY)
                    .syncWith(UUIDUtil.STREAM_CODEC, AttachmentSyncPredicate.all())
    );

    private final HashMap<UUID, DuelRequest> globalRequests;
    private final List<ServerPlayer> occupiedPlayers;
    private final HashMap<UUID, Duel> duels;

    protected DuelManager() {
        this.globalRequests = new HashMap<>();
        this.occupiedPlayers = new ArrayList<>();
        this.duels = new HashMap<>();
    }

    public static final DuelManager INSTANCE = new DuelManager();


    public boolean checkValidRequestExists(ServerPlayer requestee, ServerPlayer target) {
        UUID requesteeUuid = requestee.getUUID();
        if (!this.globalRequests.containsKey(requesteeUuid)) {
            return false;
        } else {
            UUID targetUuid = target.getUUID();
            DuelRequest request = this.globalRequests.get(requesteeUuid);
            boolean valid = this.checkValid(request);

            return valid && request.getTargetUuids().contains(targetUuid);
        }
    }

    public boolean playerHasActiveRequest(ServerPlayer requestee) {
        if (this.globalRequests.containsKey(requestee.getUUID())) {
            return checkValid(this.globalRequests.get(requestee.getUUID()));
        }
        return false;
    }

    public boolean playerOccupied(ServerPlayer player) {
        return this.occupiedPlayers.contains(player);
    }

    public boolean checkValid(DuelRequest request) {
        if (request.cancelled()) {
            this.globalRequests.remove(request.getRequesteeUuid());
            return false;
        }
        return true;
    }

    public DuelRequest requestDuel(ServerPlayer requestee, Collection<ServerPlayer> targets) {
        DuelRequest request = new DuelRequest(requestee, targets);
        this.occupiedPlayers.add(requestee);
        this.globalRequests.put(request.getRequesteeUuid(), request);
        return request;
    }

    public void cancelDuel(DuelRequest request) {
        request.markCancelled();
        this.occupiedPlayers.removeAll(request.acceptedPlayers());
        this.occupiedPlayers.remove(request.getRequestee());
    }

    public DuelRequest getRequestedDuel(ServerPlayer requestee) {
        return this.globalRequests.get(requestee.getUUID());
    }


    public void acceptDuelRequest(ServerPlayer player, DuelRequest request) {
        if (request.targetsPlayer(player)) {
            Component message = request.getAcceptedMessage(player);
            request.acceptedPlayers().forEach(p -> p.sendSystemMessage(message));
            request.markAccepted(player);
            this.occupiedPlayers.add(player);

            if (request.allPlayersAccepted()) {
                this.globalRequests.remove(request.getRequesteeUuid());

                this.createDuel(request.involvedPlayers());
            }

        }
    }

    public void declineDuelRequest(ServerPlayer player, DuelRequest request) {
        if (request.targetsPlayer(player)) {
            Component message = request.getPlayerDeclinedMessage(player);
            request.acceptedPlayers().forEach(p -> p.sendSystemMessage(message));
            this.cancelDuel(request);
        }
    }

    public void attachDuel(ServerPlayer player, Duel duel) {
        player.setAttached(DUEL_ID_ATTACHMENT, duel.getUuid());
    }

    public void detachDuel(ServerPlayer player) {
        player.setAttached(DUEL_ID_ATTACHMENT, UUID_EMPTY);
    }

    public Optional<Duel> getDuel(ServerPlayer player) {
        if (player.hasAttached(DUEL_ID_ATTACHMENT)) {
            UUID uuid = player.getAttached(DUEL_ID_ATTACHMENT);
            if (uuid != UUID_EMPTY) {
                return Optional.of(duels.get(uuid));
            }
        }

        return Optional.empty();
    }


    public UUID createDuel(Collection<ServerPlayer> players) {
        UUID id = UUID.randomUUID();
        Duel duel = new Duel(id, players);

        for (ServerPlayer player : players) {
            this.attachDuel(player, duel);
        }

        duels.put(id, duel);
        return id;
    }
}
