package dev.diamond.tappaa.command;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupRequest;
import dev.diamond.tappaa.group.QuickDuelGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 *  Messages and sources of feedback to players.
 *  <br>
 *  <br>
 *
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
public class PlayerFeedback {

    public static final SimpleCommandExceptionType ERROR_FUCK_YOU = new SimpleCommandExceptionType(Component.literal("This command deliberately threw an error. It's probably getting reworked."));
    public static final SimpleCommandExceptionType ERROR_ALREADY_IN_GROUP = new SimpleCommandExceptionType(Component.literal("You are already in a group!"));
    public static final SimpleCommandExceptionType ERROR_NOT_IN_GROUP = new SimpleCommandExceptionType(Component.literal("You are not in a group!"));
    public static final SimpleCommandExceptionType ERROR_NOT_IN_DUEL = new SimpleCommandExceptionType(Component.literal("You are not in a duel!"));
    public static final SimpleCommandExceptionType ERROR_OTHER_NOT_IN_GROUP = new SimpleCommandExceptionType(Component.literal("That player is not in a group!"));
    public static final SimpleCommandExceptionType ERROR_OTHER_NOT_IN_DUEL = new SimpleCommandExceptionType(Component.literal("That player is not in a duel!"));
    public static final SimpleCommandExceptionType ERROR_NOT_GROUP_OWNER = new SimpleCommandExceptionType(Component.literal("You are not the owner of this group!"));
    public static final SimpleCommandExceptionType ERROR_GROUP_OWNER_CANT_LEAVE = new SimpleCommandExceptionType(Component.literal("You can't leave your own group!"));
    public static final SimpleCommandExceptionType ERROR_GROUP_DOESNT_EXIST = new SimpleCommandExceptionType(Component.literal("That group does not exist!"));
    public static final SimpleCommandExceptionType ERROR_NOT_INVITED = new SimpleCommandExceptionType(Component.literal("You aren't invited to this group!"));
    public static final SimpleCommandExceptionType ERROR_CANT_INVITE_SELF = new SimpleCommandExceptionType(Component.literal("You can't invite yourself to a group!"));
    public static final SimpleCommandExceptionType ERROR_CANT_DUEL_SELF = new SimpleCommandExceptionType(Component.literal("You can't duel yourself!"));
    public static final SimpleCommandExceptionType ERROR_PLAYER_ALREADY_IN_GROUP = new SimpleCommandExceptionType(Component.literal("This player is already in your group!"));
    public static final SimpleCommandExceptionType ERROR_CANT_KICK_NON_MEMBER = new SimpleCommandExceptionType(Component.literal("You can't kick someone who isn't in your party!"));
    public static final SimpleCommandExceptionType ERROR_CANT_KICK_SELF = new SimpleCommandExceptionType(Component.literal("You can't kick yourself!"));
    public static final SimpleCommandExceptionType ERROR_ALREADY_IN_DUEL = new SimpleCommandExceptionType(Component.literal("Your group is already in a duel!"));
    public static final SimpleCommandExceptionType ERROR_OTHER_IN_GROUP = new SimpleCommandExceptionType(Component.literal("That player is already in a group!"));
    public static final SimpleCommandExceptionType ERROR_OTHER_IN_DUEL = new SimpleCommandExceptionType(Component.literal("That player is busy in a duel!"));
    public static final SimpleCommandExceptionType ERROR_CANT_EDIT_KIT = new SimpleCommandExceptionType(Component.literal("You can't edit this kit!"));
    public static final SimpleCommandExceptionType ERROR_CANT_INVITE_NOW = new SimpleCommandExceptionType(Component.literal("You can't invite players to your group right now!"));
    public static final SimpleCommandExceptionType ERROR_BAD_SLOT = new SimpleCommandExceptionType(Component.literal("You can't edit into that slot!"));
    public static final SimpleCommandExceptionType ERROR_NOT_ENOUGH_PLAYERS = new SimpleCommandExceptionType(Component.literal("There aren't enough players in the group!"));
    public static final SimpleCommandExceptionType ERROR_NOT_ENOUGH_TARGETS_FOR_INVITE = new SimpleCommandExceptionType(Component.literal("You must specify at least one player!"));
    public static final SimpleCommandExceptionType ERROR_KIT_DOESNT_EXIST = new SimpleCommandExceptionType(Component.literal("That kit does not exist!"));
    public static final SimpleCommandExceptionType ERROR_KIT_ALREADY_EXISTS = new SimpleCommandExceptionType(Component.literal("A kit already exists with that id!"));
    public static final SimpleCommandExceptionType ERROR_CANT_DELETE_KIT = new SimpleCommandExceptionType(Component.literal("That kit can't be deleted!"));

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

        String acceptCommand = "group accept " + group.getGroupUuid();
        String declineCommand = "group decline " + group.getGroupUuid();

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
                .append(Component.literal("'s " + (group instanceof QuickDuelGroup ? "group" : "duel") + " has expired.").withStyle(ChatFormatting.YELLOW));
    }


    // SOUND FEEDBACK

    public static void playYaySound(ServerPlayer target) {
        TappaaUtil.playSoundToPlayer(target, SoundEvents.NOTE_BLOCK_BANJO.value(), 1f, 1.2f);
    }

    public static void playBadSound(ServerPlayer target) {
        TappaaUtil.playSoundToPlayer(target, SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 1.0f);
    }

    public static void playDuelSound(ServerPlayer target) {
        TappaaUtil.playSoundToPlayer(target, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 1, 1.8f);
    }
}
