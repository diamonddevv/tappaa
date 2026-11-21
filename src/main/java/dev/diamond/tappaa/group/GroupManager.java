package dev.diamond.tappaa.group;

import dev.diamond.tappaa.TappaaCommand;
import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.duel.Duel;
import dev.diamond.tappaa.duel.Kit;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class GroupManager {

    private final HashMap<UUID, Group> groups;
    protected GroupManager() {
        this.groups = new HashMap<>();
    }

    public static final GroupManager INSTANCE = new GroupManager();

    public Group createGroup(ServerPlayer owner) {
        return createGroupInternal(owner, false, false);
    }

    public Group createQuickDuelGroup(MinecraftServer server, ServerPlayer owner, @Nullable Identifier kitId) {
        Group group = createGroupInternal(owner, true, kitId != null);
        if (kitId != null) {
            Optional<HashMap<Integer, ItemStack>> map = SavedKits.getSavedKits(server).getMappedItemsFromId(kitId);
            if (map.isPresent()) {
                group.getKitManager().setKit(Kit.fromMap(map.get()));
            } else {
                return null;
            }
        }
        return group;
    }

    private Group createGroupInternal(ServerPlayer owner, boolean quickDuel, boolean quickDuelUsesKit) {
        UUID uuid = UUID.randomUUID();
        Group group = new Group(uuid, owner, quickDuel, quickDuelUsesKit);
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
                TappaaCommand.playYaySound(player);
                player.sendSystemMessage(message);
            }
            player.removeAttached(Group.GROUP_ID_ATTACHMENT);
        }
        group.clearGroup();
        this.groups.remove(group.getGroupUuid());
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
