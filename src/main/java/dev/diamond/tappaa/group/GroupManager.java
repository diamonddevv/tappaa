package dev.diamond.tappaa.group;

import dev.diamond.tappaa.TappaaCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class GroupManager {

    public final HashMap<UUID, Group> groups;
    protected GroupManager() {
        this.groups = new HashMap<>();
    }

    public static final GroupManager INSTANCE = new GroupManager();

    public Group createGroup(ServerPlayer owner) {
        UUID uuid = UUID.randomUUID();
        Group group = new Group(uuid, owner);
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
        Component message = Component.literal("Your group has been disbanded.").withStyle(ChatFormatting.DARK_RED);
        for (ServerPlayer player : group.getMembers()) {
            if (!silent) {
                TappaaCommand.playBadSound(player);
                player.sendSystemMessage(message);
            }
        }
        group.clearGroup();
        this.groups.remove(group.getGroupUuid());
    }

    public static void onPlayerJoinServer(ServerPlayer player) {
        onPlayerLeaveServer(player);
    }

    public static void onPlayerLeaveServer(ServerPlayer player) {
        Optional<Group> group = GroupManager.INSTANCE.getGroup(player);
        group.ifPresent(value -> value.removeFromGroup(player));
    }
}
