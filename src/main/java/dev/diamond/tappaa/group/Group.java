package dev.diamond.tappaa.group;

import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.duel.Duel;
import dev.diamond.tappaa.duel.DuelManager;
import dev.diamond.tappaa.duel.KitManager;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Group {

    public static final AttachmentType<UUID> GROUP_ID_ATTACHMENT = AttachmentRegistry.create(
            Tappaa.id("group_id"),
            builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .copyOnDeath()
    );

    private final UUID uuid;
    private final MinecraftServer server;
    private final KitManager kit;
    private final boolean invitesExpire;
    private UUID duelUuid;
    private final UUID ownerUuid;
    private final Collection<UUID> players;
    private final ConcurrentHashMap<UUID, GroupRequest> requests;
    private final boolean quickDuelGroup;
    private final boolean quickDuelUseKit;

    public Group(UUID uuid, ServerPlayer owner, boolean quickDuelGroup, boolean quickDuelUseKit) {
        this.uuid = uuid;
        this.ownerUuid = owner.getUUID();

        this.server = owner.level().getServer();

        this.players = new ArrayList<>();
        this.requests = new ConcurrentHashMap<>(); // since we need to remove invites on the fly, this needs to be concurrent as to not throw ConcurrentModificationException
        this.duelUuid = null;
        this.invitesExpire = true;

        this.quickDuelGroup = quickDuelGroup;
        this.quickDuelUseKit = quickDuelUseKit;

        this.kit = new KitManager();

        addToGroup(owner);
    }

    public UUID getGroupUuid() {
        return uuid;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public ServerPlayer getOwner() {
        return server.getPlayerList().getPlayer(ownerUuid);
    }

    public KitManager getKitManager() {
        return kit;
    }

    public boolean isQuickDuelGroup() {
        return quickDuelGroup;
    }

    public boolean invitesExpire() {
        return invitesExpire;
    }

    public Collection<ServerPlayer> getMembers() {
        return players.stream().map(server.getPlayerList()::getPlayer).toList();
    }

    public Collection<ServerPlayer> getRequestedMembers() {
        return requests.values().stream().map(GroupRequest::getTargetPlayer).toList();
    }

    public void setDuelUuid(UUID uuid) {
        this.duelUuid = uuid;
    }

    public boolean hasValidActiveDuel() {
        return duelUuid != null && DuelManager.INSTANCE.findDuel(duelUuid).isPresent();
    }

    public Optional<Duel> getActiveDuel() {
        if (duelUuid == null) return Optional.empty();
        return DuelManager.INSTANCE.findDuel(duelUuid);
    }

    public void addToGroup(ServerPlayer player) {
        if (player.hasAttached(GROUP_ID_ATTACHMENT)) {
            return;
        }
        player.setAttached(GROUP_ID_ATTACHMENT, this.uuid);
        this.players.add(player.getUUID());
    }

    public void removeFromGroup(ServerPlayer player) {
        if (player.hasAttached(GROUP_ID_ATTACHMENT) && player.getAttached(GROUP_ID_ATTACHMENT) == uuid) {
            player.removeAttached(GROUP_ID_ATTACHMENT);
            this.players.remove(player.getUUID());
        }
    }

    public void clearGroup() {
        for (ServerPlayer player : getMembers()) {
            if (player.hasAttached(GROUP_ID_ATTACHMENT) && player.getAttached(GROUP_ID_ATTACHMENT) == uuid) {
                player.removeAttached(GROUP_ID_ATTACHMENT);
            }
        }
        players.clear();
    }

    public boolean hasMember(ServerPlayer player) {
        return players.contains(player.getUUID());
    }

    public void broadcastMessage(Component message, boolean includeOwner) {
        for (ServerPlayer member : getMembers()) {
            if (includeOwner || member.getUUID() != ownerUuid) {
                member.sendSystemMessage(message);
            }
        }
    }

    public void forEachMember(Consumer<ServerPlayer> action, boolean includeOwner) {
        for (ServerPlayer member : getMembers()) {
            if (includeOwner || member.getUUID() != ownerUuid) {
                action.accept(member);
            }
        }
    }

    public void invite(ServerPlayer player) {
        requests.put(player.getUUID(), new GroupRequest(this, player.getUUID(), this.invitesExpire ? GroupRequest.REQUEST_TIMEOUT_DURATION : -1));
    }

    public void onRespondInvite() {
        if (isQuickDuelGroup()) {
            if (requests.isEmpty()) {
                if (players.size() > 1) {
                    DuelManager.INSTANCE.createDuelFromGroup(this, true, quickDuelUseKit);
                } else {
                    GroupManager.INSTANCE.disbandGroup(this, false);
                }
            }
        }
    }

    public void tick() {
        this.requests.values().forEach(GroupRequest::tick);
    }

    public void rescindInvite(ServerPlayer player) {
        this.rescindInvite(player.getUUID());
    }

    public void rescindInvite(UUID playerUuid) {
        requests.remove(playerUuid);
        this.onRespondInvite();
    }

    public void rescindAllInvites() {
        this.requests.keySet().forEach(this::rescindInvite);
    }

    public boolean isPlayerInvited(ServerPlayer player) {
        return requests.containsKey(player.getUUID());
    }


}
