package dev.diamond.tappaa.group;

import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.command.PlayerFeedback;
import dev.diamond.tappaa.duel.Kit;
import dev.diamond.tappaa.serial.SavedKits;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager {

    private final ConcurrentHashMap<UUID, Group> groups;
    protected GroupManager() {
        this.groups = new ConcurrentHashMap<>();
    }

    public static final GroupManager INSTANCE = new GroupManager();

    public Group createGroup(ServerPlayer owner) {
        UUID uuid = UUID.randomUUID();
        Group group = new Group(uuid, owner);
        this.groups.put(uuid, group);
        return group;
    }

    public Group createQuickDuelGroup(ServerPlayer owner, Kit kit) {
        UUID uuid = UUID.randomUUID();
        Group group = new QuickDuelGroup(uuid, owner, kit);
        this.groups.put(uuid, group);
        return group;
    }

    /**
     * Gets the players group, if they are in one. If the player has a group attachment, but the group attachment points to nothing, this function also removes that attachment.
     * @param player The player, whose group to fetch.
     * @return The group of the player, if it exists.
     */
    public Optional<Group> getGroup(ServerPlayer player) {
        if (player.hasAttached(Group.GROUP_ID_ATTACHMENT)) {
            UUID uuid = player.getAttached(Group.GROUP_ID_ATTACHMENT);
            if (uuid != null) {
                if (!groups.containsKey(uuid)) {
                    player.removeAttached(Group.GROUP_ID_ATTACHMENT);
                    return Optional.empty();
                }
                return Optional.of(groups.get(uuid));
            }
        }

        return Optional.empty();
    }

    public Optional<Group> findGroup(UUID uuid) {
        return Optional.ofNullable(groups.get(uuid));
    }

    public void disbandGroup(Group group, boolean silent) {
        Component message = Component.literal("Your group has been disbanded.").withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer player : group.getMembers()) {
            if (!silent) {
                PlayerFeedback.playYaySound(player);
                player.sendSystemMessage(message);
            }
            player.removeAttached(Group.GROUP_ID_ATTACHMENT);
        }
        group.rescindAllInvites();
        group.clearGroup();
        this.groups.remove(group.getGroupUuid());
    }

    public static void registerGroupEvents() {
        ServerPlayerEvents.JOIN.register(GroupManager::onPlayerJoinServer);
        ServerPlayerEvents.LEAVE.register(GroupManager::onPlayerLeaveServer);
        ServerTickEvents.START_SERVER_TICK.register(GroupManager::onServerTicks);
    }

    private static void onServerTicks(MinecraftServer server) {
        GroupManager.INSTANCE.groups.values().forEach(Group::tick);
    }

    public static void onPlayerJoinServer(ServerPlayer player) {
        GroupManager.INSTANCE.getGroup(player).ifPresent(group -> group.removeFromGroup(player));
    }

    public static void onPlayerLeaveServer(ServerPlayer player) {
        Optional<Group> group = GroupManager.INSTANCE.getGroup(player);
        group.ifPresent(value -> {
            if (value.getOwner().getUUID() == player.getUUID()) {
                value.getActiveDuel().ifPresent(duel -> duel.endDuel(true));
                GroupManager.INSTANCE.disbandGroup(value, false);
            } else {
                value.forEachMember(member -> member.sendSystemMessage(
                        Component.empty()
                                .append(TappaaUtil.playerTextComponent(player, ChatFormatting.GOLD, ChatFormatting.ITALIC))
                                .append(Component.literal(" was removed from the group, as they left the server.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                ), true);
                value.removeFromGroup(player);
            }
        });
    }
}
