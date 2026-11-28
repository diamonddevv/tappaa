package dev.diamond.tappaa.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.duel.AvailableKitsSuggestionProvider;
import dev.diamond.tappaa.duel.DuelManager;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupManager;
import dev.diamond.tappaa.group.QuickDuelGroup;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GroupCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {
        dispatcher.register(
                literal("group").then(
                        literal("create").executes(GroupCommand::createGroup)
                ).then(
                        literal("disband").executes(GroupCommand::disbandGroup)
                ).then(
                        literal("invite").then(
                                argument("players", EntityArgument.players()).executes(GroupCommand::inviteToGroup)
                        )
                ).then(
                        literal("leave").executes(GroupCommand::leaveGroup)
                ).then(
                        literal("list").executes(GroupCommand::listGroup)
                ).then(
                        literal("accept").then(
                                argument("uuid", UuidArgument.uuid()).executes(GroupCommand::acceptInvite)
                        )
                ).then(
                        literal("decline").then(
                                argument("uuid", UuidArgument.uuid()).executes(GroupCommand::declineInvite)
                        )
                ).then(
                        literal("kick").then(
                                argument("player", EntityArgument.player()).executes(GroupCommand::kickMemberFromGroup)
                        )
                )
        );
    }


    private static int createGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (GroupManager.INSTANCE.getGroup(src).isPresent()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_ALREADY_IN_GROUP.create();
        }

        GroupManager.INSTANCE.createGroup(src);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.literal("Successfully created group.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int disbandGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        GroupManager.INSTANCE.disbandGroup(group.get(), false); // this sends feedback to the person running the command too
        return 1;
    }

    private static int inviteToGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get() instanceof QuickDuelGroup) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_INVITE_NOW.create();
        }

        List<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "players").stream().toList();

        if (targets.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_ENOUGH_TARGETS_FOR_INVITE.create();
        }

        boolean onlyOne = targets.size() == 1;

        int failures = 0;
        for (ServerPlayer target : targets) {
            if (src == target) {
                failures += 1;
                if (onlyOne) {
                    PlayerFeedback.playBadSound(src);
                    throw PlayerFeedback.ERROR_CANT_INVITE_SELF.create();
                }
                continue;
            }

            if (group.get().hasMember(target)) {
                failures += 1;
                if (onlyOne) {
                    PlayerFeedback.playBadSound(src);
                    throw PlayerFeedback.ERROR_PLAYER_ALREADY_IN_GROUP.create();
                }
                continue;
            }

            if (DuelManager.INSTANCE.getDuel(target).isPresent()) {
                failures += 1;
                if (onlyOne) {
                    PlayerFeedback.playBadSound(src);
                    throw PlayerFeedback.ERROR_CANT_INVITE_NOW.create();
                }
                continue;
            }

            group.get().invite(target);
            target.sendSystemMessage(PlayerFeedback.target_InvitedToGroupRequest(group.get(), false));


            group.get().broadcastMessage(PlayerFeedback.broadcast_InvitedToGroup(group.get(), target), false);
        }

        if (failures == targets.size()) {
            PlayerFeedback.playBadSound(src);
            src.sendSystemMessage(Component.literal("Failed to send all requests.").withStyle(ChatFormatting.DARK_RED));
        } else if (targets.size() == 1) {
            PlayerFeedback.playYaySound(src);
            src.sendSystemMessage(PlayerFeedback.source_InvitedToGroup(targets.getFirst()));
        } else {
            PlayerFeedback.playYaySound(src);
            src.sendSystemMessage(Component.literal("Sent " + (targets.size() - failures) + " requests.").withStyle(ChatFormatting.GREEN));
        }

        return 1;
    }

    private static int leaveGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() == src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_GROUP_OWNER_CANT_LEAVE.create();
        }

        group.get().removeFromGroup(src);
        src.sendSystemMessage(Component.literal("Left group.").withStyle(ChatFormatting.YELLOW));
        group.get().broadcastMessage(PlayerFeedback.broadcast_LeftGroup(src), true);

        return 1;
    }

    private static int listGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        src.sendSystemMessage(Component.empty()
                .append(TappaaUtil.playerTextComponent(group.get().getOwner(), ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GOLD))
                .append(Component.literal("'s group:").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GOLD)))
        ;
        for (ServerPlayer player : group.get().getMembers()) {
            src.sendSystemMessage(TappaaUtil.playerTextComponent(player));
        }
        for (ServerPlayer player : group.get().getRequestedMembers()) {
            src.sendSystemMessage(TappaaUtil.playerTextComponent(player, ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        return 1;
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (GroupManager.INSTANCE.getGroup(src).isPresent()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_ALREADY_IN_GROUP.create();
        }

        UUID uuid = UuidArgument.getUuid(ctx, "uuid");
        Optional<Group> group = GroupManager.INSTANCE.findGroup(uuid);

        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_GROUP_DOESNT_EXIST.create();
        }

        if (!group.get().isPlayerInvited(src)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_INVITED.create();
        }

        src.sendSystemMessage(PlayerFeedback.source_JoinedGroup(group.get()));
        PlayerFeedback.playYaySound(src);

        group.get().broadcastMessage(PlayerFeedback.broadcast_JoinedGroup(src), true);
        group.get().forEachMember(PlayerFeedback::playYaySound, true);

        group.get().addToGroup(src);
        group.get().rescindInvite(src);

        return 1;
    }

    private static int declineInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        UUID uuid = UuidArgument.getUuid(ctx, "uuid");
        Optional<Group> group = GroupManager.INSTANCE.findGroup(uuid);

        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_GROUP_DOESNT_EXIST.create();
        }

        if (!group.get().isPlayerInvited(src)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_INVITED.create();
        }

        src.sendSystemMessage(PlayerFeedback.source_LeftGroup(group.get()));
        PlayerFeedback.playBadSound(src);

        Component msg = PlayerFeedback.broadcast_DeclinedInvite(src);
        group.get().forEachMember(member -> {
            member.sendSystemMessage(msg);
            PlayerFeedback.playBadSound(member);
        }, true);

        group.get().rescindInvite(src);

        return 1;
    }

    private static int kickMemberFromGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        if (!group.get().hasMember(target)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_KICK_NON_MEMBER.create();
        }

        if (src == target) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_KICK_SELF.create();
        }

        group.get().removeFromGroup(target);

        target.sendSystemMessage(PlayerFeedback.target_KickedFromGroup(group.get()));
        PlayerFeedback.playBadSound(target);

        group.get().broadcastMessage(PlayerFeedback.broadcast_KickedFromGroup(target), true);
        group.get().forEachMember(PlayerFeedback::playBadSound, true);

        return 1;
    }
}
