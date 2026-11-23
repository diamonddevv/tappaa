package dev.diamond.tappaa;

import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupRequest;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

/**
 *  Contains all the messages used in the mod. Doesn't send anything, just builds components. Eventually, these will be migrated to
 *  use translation keys with fallbacks.
 *
 *  <h4>Method Name Intended Recipients</h4>
 *  <ul>
 *      <li><i>source</i>: the person who executed the command that caused this (eg. inviter of a player to a group)</li>
 *      <li><i>broadcast</i>: everyone else who needs to receive the information (eg. players in a group)</li>
 *      <li><i>target</i>: the person who was the target of the command (eg. invited player)</li>
 *  </ul>
 *
 *  Method descriptions should try to include the intended translation key, for when that is written.
 */
public class ComponentMessages {

    /**
     * msg.tappaa.group.invited.source
     */
    public static Component source_InvitedToGroup(ServerPlayer target) {
        return Component.empty()
                .append(Component.literal("Invited ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" to group.").withStyle(ChatFormatting.GREEN))
                ;
    }

    /**
     * msg.tappaa.group.invited.broadcast
     */
    public static Component broadcast_InvitedToGroup(Group group, ServerPlayer target) {
        return Component.empty()
                .append(TappaaUtil.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal(" invited ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" to group.").withStyle(ChatFormatting.GREEN))
                ;
    }

    /**
     * WILL NEED EXTRA WORK
     */
    public static Component target_InvitedToGroupRequest(Group group, boolean duel) {

        String acceptCommand = "tappaa group accept " + group.getGroupUuid();
        String declineCommand = "tappaa group decline " + group.getGroupUuid();

        return Component.empty()
                .append(TappaaUtil.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal(
                        " has invited you to " + (duel ? "a duel." : "join their group.")
                ).withColor(TextColor.GREEN))
                .append(Component.literal(
                        (group.invitesExpire() ? " [Invite expires in " + ((int) Math.floor(GroupRequest.REQUEST_TIMEOUT_DURATION / 20f)) + "s]" : "uagwdkujaf") + "\n"
                ).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY))
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

    /**
     * msg.tappaa.group.joined.broadcast
     */
    public static Component broadcast_JoinedGroup(ServerPlayer target) {
        return Component.empty()
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" joined the group.").withColor(TextColor.GREEN))
                ;
    }

    /**
     * msg.tappaa.group.left.broadcast
     */
    public static Component broadcast_LeftGroup(ServerPlayer target) {
        return Component.empty()
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" left the group.").withColor(TextColor.YELLOW))
                ;
    }

    /**
     * msg.tappaa.group.joined.source
     */
    public static Component source_JoinedGroup(Group group) {
        return Component.empty()
                .append(Component.literal("Joined ").withColor(TextColor.GREEN))
                .append(TappaaUtil.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("'s group.").withColor(TextColor.GREEN))
                ;
    }

    /**
     * msg.tappaa.group.left.source
     */
    public static Component source_LeftGroup(Group group) {
        return Component.empty()
                .append(Component.literal("Declined ").withColor(TextColor.GREEN))
                .append(TappaaUtil.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("'s group invite.").withColor(TextColor.GREEN))
                ;
    }

    /**
     * msg.tappaa.group.declined.broadcast
     */
    public static Component broadcast_DeclinedInvite(ServerPlayer target) {
        return Component.empty()
                .append(TappaaUtil.playerTextComponent(target))
                .append(Component.literal(" declined to join the group.").withStyle(ChatFormatting.RED))
                ;
    }

    /**
     * msg.tappaa.group.kicked.target
     */
    public static Component target_KickedFromGroup(Group group) {
        return Component.empty()
                .append(Component.literal("You have been kicked from ").withStyle(ChatFormatting.DARK_RED))
                .append(TappaaUtil.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("'s group.").withStyle(ChatFormatting.DARK_RED))
                ;
    }

    /**
     * msg.tappaa.group.kicked.broadcast
     */
    public static Component broadcast_KickedFromGroup(ServerPlayer target) {
        return Component.empty()
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" was kicked from the group.").withStyle(ChatFormatting.DARK_RED))
                ;
    }

    /**
     * msg.tappaa.quickduel.request.single.source
     */
    public static Component source_SentQuickDuelRequestSingle(ServerPlayer target) {
        return Component.empty()
                .append(Component.literal("Invited ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" to duel.").withStyle(ChatFormatting.GREEN))
                ;
    }

    /**
     * msg.tappaa.request.expired.target
     */
    public static Component target_RequestExpired(Group group) {
        return Component.empty()
                .append(Component.literal("Your invite to ").withStyle(ChatFormatting.YELLOW))
                .append(TappaaUtil.playerTextComponent(group.getOwner(), ChatFormatting.GOLD))
                .append(Component.literal("'s " + (group.isQuickDuelGroup() ? "duel" : "group") + " has expired.").withStyle(ChatFormatting.YELLOW));
    }
}
