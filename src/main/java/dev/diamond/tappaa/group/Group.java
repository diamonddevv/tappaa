package dev.diamond.tappaa.group;

import dev.diamond.tappaa.Tappaa;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class Group {

    public static final AttachmentType<UUID> GROUP_ID_ATTACHMENT = AttachmentRegistry.create(
            Tappaa.id("group_id"),
            builder -> builder
                    .persistent(UUIDUtil.CODEC)
    );

    private final UUID uuid;
    private ServerPlayer owner;
    private final Collection<ServerPlayer> players;
    private final Collection<ServerPlayer> invites;

    public Group(UUID uuid, ServerPlayer owner) {
        this.uuid = uuid;
        this.owner = owner;
        this.players = new ArrayList<>();
        this.invites = new ArrayList<>();

        addToGroup(owner);
    }

    public UUID getGroupUuid() {
        return uuid;
    }

    public ServerPlayer getOwner() {
        return owner;
    }

    public Collection<ServerPlayer> getMembers() {
        return players;
    }

    public void addToGroup(ServerPlayer player) {
        if (player.hasAttached(GROUP_ID_ATTACHMENT)) {
            return;
        }
        player.setAttached(GROUP_ID_ATTACHMENT, this.uuid);
        this.players.add(player);
    }

    public void removeFromGroup(ServerPlayer player) {
        if (player.hasAttached(GROUP_ID_ATTACHMENT) && player.getAttached(GROUP_ID_ATTACHMENT) == uuid) {
            player.removeAttached(GROUP_ID_ATTACHMENT);
            this.players.remove(player);
        }
    }

    public void clearGroup() {
        for (ServerPlayer player : players) {
            if (player.hasAttached(GROUP_ID_ATTACHMENT) && player.getAttached(GROUP_ID_ATTACHMENT) == uuid) {
                player.removeAttached(GROUP_ID_ATTACHMENT);
            }
        }
        players.clear();
    }

    public boolean hasMember(ServerPlayer player) {
        return players.contains(player);
    }

    public void broadcastMessage(Component message, boolean includeOwner) {
        for (ServerPlayer member : getMembers()) {
            if (includeOwner || member != owner) {
                member.sendSystemMessage(message);
            }
        }
    }

    public void forEachMember(Consumer<ServerPlayer> action, boolean includeOwner) {
        for (ServerPlayer member : getMembers()) {
            if (includeOwner || member != owner) {
                action.accept(member);
            }
        }
    }

    public void invite(ServerPlayer player) {
        invites.add(player);
    }

    public void rescindInvite(ServerPlayer player) {
        invites.remove(player);
    }

    public boolean isPlayerInvited(ServerPlayer player) {
        return invites.contains(player);
    }


    public static Component getSentInviteMessageOwner(ServerPlayer target) {
        return Component.empty()
                .append(Component.literal("Invited ").withStyle(ChatFormatting.GREEN))
                .append(Tappaa.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" to group.").withStyle(ChatFormatting.GREEN))
                ;
    }

    public static Component getSentInviteMessageGroup(Group group, ServerPlayer target) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("invited ").withStyle(ChatFormatting.GREEN))
                .append(Tappaa.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" to group.").withStyle(ChatFormatting.GREEN))
                ;
    }

    public static Component getIncomingInviteMessage(Group group) {

        String acceptCommand = "tappaa group accept " + group.getGroupUuid();
        String declineCommand = "tappaa group decline " + group.getGroupUuid();

        return Component.empty()
                .append(Tappaa.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal(" has invited you to join their duel group. ").withColor(TextColor.GREEN))
                .append(Component.literal("[ACCEPT]")
                        .withColor(TextColor.LIGHT_PURPLE)
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(acceptCommand))))
                .append(" ")
                .append(Component.literal("[DECLINE]")
                        .withColor(TextColor.DARK_RED)
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(declineCommand))))
                ;
    }

    public static Component getJoinedMessage(ServerPlayer target) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" joined the group.").withColor(TextColor.GREEN))
                ;
    }

    public static Component getLeftMessage(ServerPlayer target) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" left the group.").withColor(TextColor.YELLOW))
                ;
    }

    public static Component getAcceptMessage(Group group) {
        return Component.empty()
                .append(Component.literal("Joined ").withColor(TextColor.GREEN))
                .append(Tappaa.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("'s group.").withColor(TextColor.GREEN))
                ;
    }

    public static Component getDeclinedMessage(Group group) {
        return Component.empty()
                .append(Component.literal("Declined ").withColor(TextColor.GREEN))
                .append(Tappaa.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("'s group invite.").withColor(TextColor.GREEN))
                ;
    }

    public static Component getRejectedMessage(ServerPlayer target) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(target))
                .append(Component.literal(" declined to join the group.").withStyle(ChatFormatting.RED))
                ;
    }

    public static Component getKickedMessage(Group group) {
        return Component.empty()
                .append(Component.literal("You have been kicked from ").withStyle(ChatFormatting.DARK_RED))
                .append(Tappaa.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("'s party.").withStyle(ChatFormatting.DARK_RED))
                ;
    }

    public static Component getKickedBroadcastMessage(ServerPlayer target) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" was kicked from the party.").withStyle(ChatFormatting.DARK_RED))
                ;
    }
}
