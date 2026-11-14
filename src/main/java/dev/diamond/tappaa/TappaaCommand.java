package dev.diamond.tappaa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.diamond.tappaa.duel.DuelManager;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class TappaaCommand {

    private static final SimpleCommandExceptionType ERROR_ALREADY_IN_GROUP = new SimpleCommandExceptionType(Component.literal("You are already in a group!"));
    private static final SimpleCommandExceptionType ERROR_NOT_IN_GROUP = new SimpleCommandExceptionType(Component.literal("You are not in a group!"));
    private static final SimpleCommandExceptionType ERROR_NOT_GROUP_OWNER = new SimpleCommandExceptionType(Component.literal("You are not the owner of this group!"));
    private static final SimpleCommandExceptionType ERROR_GROUP_OWNER_CANT_LEAVE = new SimpleCommandExceptionType(Component.literal("You can't leave your own group!"));
    private static final SimpleCommandExceptionType ERROR_GROUP_DOESNT_EXIST = new SimpleCommandExceptionType(Component.literal("That group does not exist!"));
    private static final SimpleCommandExceptionType ERROR_NOT_INVITED = new SimpleCommandExceptionType(Component.literal("You aren't invited to this group!"));
    private static final SimpleCommandExceptionType ERROR_CANT_INVITE_SELF = new SimpleCommandExceptionType(Component.literal("You can't invite yourself to a group!"));
    private static final SimpleCommandExceptionType ERROR_PLAYER_ALREADY_IN_GROUP = new SimpleCommandExceptionType(Component.literal("This player is already in your group!"));
    private static final SimpleCommandExceptionType ERROR_CANT_KICK_NON_MEMBER = new SimpleCommandExceptionType(Component.literal("You can't kick someone who isn't in your party!"));
    private static final SimpleCommandExceptionType ERROR_CANT_KICK_SELF = new SimpleCommandExceptionType(Component.literal("You can't kick yourself!"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {

        dispatcher.register(
                literal("tappaa").then(
                        literal("group").then(
                                literal("create").executes(TappaaCommand::createGroup)
                        ).then(
                                literal("disband").executes(TappaaCommand::disbandGroup)
                        ).then(
                                literal("invite").then(
                                        argument("player", EntityArgument.player()).executes(TappaaCommand::inviteToGroup)
                                )
                        ).then(
                                literal("leave").executes(TappaaCommand::leaveGroup)
                        ).then(
                                literal("list").executes(TappaaCommand::listGroup)
                        ).then(
                                literal("accept").then(
                                        argument("uuid", UuidArgument.uuid()).executes(TappaaCommand::acceptInvite)
                                )
                        ).then(
                                literal("decline").then(
                                        argument("uuid", UuidArgument.uuid()).executes(TappaaCommand::declineInvite)
                                )
                        ).then(
                                literal("kick").then(
                                        argument("player", EntityArgument.player()).executes(TappaaCommand::kickMemberFromGroup)
                                )
                        ).then(
                                literal("duel").executes(TappaaCommand::duelGroup)
                        )
                )
        );


    }

    //

    private static boolean inGroup(CommandSourceStack src) {
        if (!src.isPlayer()) {
            return false;
        }
        ServerPlayer player = src.getPlayer();
        assert player != null;
        return GroupManager.INSTANCE.getGroup(player).isPresent();
    }

    private static boolean ownsGroup(CommandSourceStack src) {
        if (!src.isPlayer()) {
            return false;
        }

        ServerPlayer player = src.getPlayer();
        assert player != null;

        Optional<Group> group = GroupManager.INSTANCE.getGroup(player);
        return group.filter(value -> value.getOwner() == player).isPresent();
    }

    //

    public static void playYaySound(ServerPlayer target) {
        target.connection.send(new ClientboundSoundPacket(
                SoundEvents.NOTE_BLOCK_BANJO,
                SoundSource.MASTER,
                target.getX(), target.getY(), target.getZ(),
                1.0f, 1.2f, target.getRandom().nextLong()
                )
        );
    }

    public static void playBadSound(ServerPlayer target) {
        target.connection.send(new ClientboundSoundPacket(
                        SoundEvents.NOTE_BLOCK_BASS,
                        SoundSource.MASTER,
                        target.getX(), target.getY(), target.getZ(),
                        1.0f, 1.0f, target.getRandom().nextLong()
                )
        );
    }

    //

    private static int createGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (GroupManager.INSTANCE.getGroup(src).isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_GROUP.create();
        }

        GroupManager.INSTANCE.createGroup(src);

        playYaySound(src);
        src.sendSystemMessage(Component.literal("Succesfully created group.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int disbandGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            playBadSound(src);
            throw ERROR_NOT_GROUP_OWNER.create();
        }

        GroupManager.INSTANCE.disbandGroup(group.get(), false); // this sends feedback to the person running the command too
        return 1;
    }

    private static int inviteToGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            playBadSound(src);
            throw ERROR_NOT_GROUP_OWNER.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        if (src == target) {
            playBadSound(src);
            throw ERROR_CANT_INVITE_SELF.create();
        }

        if (group.get().hasMember(target)) {
            playBadSound(src);
            throw ERROR_PLAYER_ALREADY_IN_GROUP.create();
        }

        group.get().invite(target);
        target.sendSystemMessage(Group.getIncomingInviteMessage(group.get()));

        src.sendSystemMessage(Group.getSentInviteMessageOwner(target));
        playYaySound(src);
        group.get().broadcastMessage(Group.getSentInviteMessageGroup(group.get(), target), false);

        return 1;
    }

    private static int leaveGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() == src) {
            playBadSound(src);
            throw ERROR_GROUP_OWNER_CANT_LEAVE.create();
        }

        group.get().removeFromGroup(src);
        src.sendSystemMessage(Component.literal("Left group.").withStyle(ChatFormatting.YELLOW));
        group.get().broadcastMessage(Group.getLeftMessage(src), true);

        return 1;
    }

    private static int listGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        src.sendSystemMessage(Component.empty()
                .append(Tappaa.playerTextComponent(group.get().getOwner(), ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GOLD))
                .append(Component.literal("'s group:").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GOLD)))
        ;
        for (ServerPlayer player : group.get().getMembers()) {
            src.sendSystemMessage(Tappaa.playerTextComponent(player, ChatFormatting.ITALIC));
        }
        return 1;
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (GroupManager.INSTANCE.getGroup(src).isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_GROUP.create();
        }

        UUID uuid = UuidArgument.getUuid(ctx, "uuid");
        Optional<Group> group = GroupManager.INSTANCE.findGroup(uuid);

        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_GROUP_DOESNT_EXIST.create();
        }

        if (!group.get().isPlayerInvited(src)) {
            playBadSound(src);
            throw ERROR_NOT_INVITED.create();
        }

        src.sendSystemMessage(Group.getAcceptMessage(group.get()));
        playYaySound(src);

        group.get().broadcastMessage(Group.getJoinedMessage(src), true);
        group.get().forEachMember(TappaaCommand::playYaySound, true);

        group.get().addToGroup(src);
        group.get().rescindInvite(src);

        return 1;
    }

    private static int declineInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        UUID uuid = UuidArgument.getUuid(ctx, "uuid");
        Optional<Group> group = GroupManager.INSTANCE.findGroup(uuid);

        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_GROUP_DOESNT_EXIST.create();
        }

        if (!group.get().isPlayerInvited(src)) {
            playBadSound(src);
            throw ERROR_NOT_INVITED.create();
        }

        src.sendSystemMessage(Group.getDeclinedMessage(group.get()));
        playBadSound(src);

        group.get().broadcastMessage(Group.getRejectedMessage(src), true);
        group.get().forEachMember(TappaaCommand::playBadSound, true);

        group.get().rescindInvite(src);

        return 1;
    }

    private static int kickMemberFromGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            playBadSound(src);
            throw ERROR_NOT_GROUP_OWNER.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        if (!group.get().hasMember(target)) {
            playBadSound(src);
            throw ERROR_CANT_KICK_NON_MEMBER.create();
        }

        if (src == target) {
            playBadSound(src);
            throw ERROR_CANT_KICK_SELF.create();
        }

        group.get().removeFromGroup(target);

        src.sendSystemMessage(Group.getKickedMessage(group.get()));
        playBadSound(src);

        group.get().broadcastMessage(Group.getKickedBroadcastMessage(target), true);
        group.get().forEachMember(TappaaCommand::playBadSound, true);

        return 1;
    }

    private static int duelGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            playBadSound(src);
            throw ERROR_NOT_GROUP_OWNER.create();
        }

        DuelManager.INSTANCE.createDuelFromGroup(group.get());

        src.sendSystemMessage(Component.literal("Dueling group!").withStyle(ChatFormatting.GREEN));
        playYaySound(src);

        return 1;
    }
}
