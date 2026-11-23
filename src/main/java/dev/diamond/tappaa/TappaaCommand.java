package dev.diamond.tappaa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.diamond.tappaa.duel.*;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupManager;
import dev.diamond.tappaa.serial.SavedKits;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class TappaaCommand {

    // ill probably seperate these into multiple commands, this file is getting huge..

    private static final SimpleCommandExceptionType ERROR_ALREADY_IN_GROUP = new SimpleCommandExceptionType(Component.literal("You are already in a group!"));
    private static final SimpleCommandExceptionType ERROR_NOT_IN_GROUP = new SimpleCommandExceptionType(Component.literal("You are not in a group!"));
    private static final SimpleCommandExceptionType ERROR_NOT_IN_DUEL = new SimpleCommandExceptionType(Component.literal("You are not in a duel!"));
    private static final SimpleCommandExceptionType ERROR_OTHER_NOT_IN_GROUP = new SimpleCommandExceptionType(Component.literal("That player is not in a group!"));
    private static final SimpleCommandExceptionType ERROR_OTHER_NOT_IN_DUEL = new SimpleCommandExceptionType(Component.literal("That player is not in a duel!"));
    private static final SimpleCommandExceptionType ERROR_NOT_GROUP_OWNER = new SimpleCommandExceptionType(Component.literal("You are not the owner of this group!"));
    private static final SimpleCommandExceptionType ERROR_GROUP_OWNER_CANT_LEAVE = new SimpleCommandExceptionType(Component.literal("You can't leave your own group!"));
    private static final SimpleCommandExceptionType ERROR_GROUP_DOESNT_EXIST = new SimpleCommandExceptionType(Component.literal("That group does not exist!"));
    private static final SimpleCommandExceptionType ERROR_NOT_INVITED = new SimpleCommandExceptionType(Component.literal("You aren't invited to this group!"));
    private static final SimpleCommandExceptionType ERROR_CANT_INVITE_SELF = new SimpleCommandExceptionType(Component.literal("You can't invite yourself to a group!"));
    private static final SimpleCommandExceptionType ERROR_PLAYER_ALREADY_IN_GROUP = new SimpleCommandExceptionType(Component.literal("This player is already in your group!"));
    private static final SimpleCommandExceptionType ERROR_CANT_KICK_NON_MEMBER = new SimpleCommandExceptionType(Component.literal("You can't kick someone who isn't in your party!"));
    private static final SimpleCommandExceptionType ERROR_CANT_KICK_SELF = new SimpleCommandExceptionType(Component.literal("You can't kick yourself!"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_IN_DUEL = new SimpleCommandExceptionType(Component.literal("Your group is already in a duel!"));
    private static final SimpleCommandExceptionType ERROR_OTHER_IN_GROUP = new SimpleCommandExceptionType(Component.literal("That player is already in a group!"));
    private static final SimpleCommandExceptionType ERROR_OTHER_IN_DUEL = new SimpleCommandExceptionType(Component.literal("That player is busy in a duel!"));
    private static final SimpleCommandExceptionType ERROR_CANT_EDIT_KIT_IN_DUEL = new SimpleCommandExceptionType(Component.literal("You can't edit the group kit when you're in the middle of a duel!"));
    private static final SimpleCommandExceptionType ERROR_CANT_EDIT_KIT_NO_PERMISSION = new SimpleCommandExceptionType(Component.literal("You can't edit the kit using the UI, you lack the permission."));
    private static final SimpleCommandExceptionType ERROR_CANT_INVITE_NOW = new SimpleCommandExceptionType(Component.literal("You can't invite players to your group right now!"));
    private static final SimpleCommandExceptionType ERROR_BAD_SLOT = new SimpleCommandExceptionType(Component.literal("You can't edit into that slot!"));
    private static final SimpleCommandExceptionType ERROR_NOT_ENOUGH_PLAYERS = new SimpleCommandExceptionType(Component.literal("There aren't enough players in the group!"));
    private static final SimpleCommandExceptionType ERROR_NOT_ENOUGH_TARGETS_FOR_INVITE = new SimpleCommandExceptionType(Component.literal("You must specify at least one player!"));
    private static final SimpleCommandExceptionType ERROR_KIT_DOESNT_EXIST = new SimpleCommandExceptionType(Component.literal("That kit does not exist!"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {

        dispatcher.register(
                literal("tappaa").then(
                        literal("group").then(
                                literal("create").executes(TappaaCommand::createGroup)
                        ).then(
                                literal("disband").executes(TappaaCommand::disbandGroup)
                        ).then(
                                literal("invite").then(
                                        argument("players", EntityArgument.players()).executes(TappaaCommand::inviteToGroup)
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
                        )
                ).then(
                        literal("dev").requires(src -> FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment() || src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))).then(
                                literal("forcegroup").then(
                                        argument("players", EntityArgument.players()).executes(TappaaCommand::devForceCreateGroup)
                                )
                        ).then(
                                literal("uuid").then(
                                        argument("player", EntityArgument.player()).then(
                                                literal("group").executes(TappaaCommand::devGetGroupUuid)
                                        ).then(
                                                literal("duel").executes(TappaaCommand::devGetDuelUuid)
                                        )
                                )
                        ).then(
                                literal("forceinvite").then(
                                        argument("players", EntityArgument.players()).executes(TappaaCommand::devForceInvite)
                                )
                        ).then(
                                literal("forceduel").then(
                                        argument("player", EntityArgument.player()).executes(TappaaCommand::devForceDuelDirect)
                                )
                        )
                ).then(
                        literal("duel").then(
                                literal("request").then(
                                        argument("players", EntityArgument.players()).executes(TappaaCommand::duelDirectMultiple).then(
                                                argument("kit", IdentifierArgument.id())
                                                        .suggests(new AvailableKitsSuggestionProvider())
                                                        .executes(TappaaCommand::duelDirectMultiple)
                                        )
                                )
                        ).then(
                                literal("group").executes(c -> TappaaCommand.duelGroup(c, true)).then(
                                        literal("nokit").executes(c -> TappaaCommand.duelGroup(c, false))
                                )
                        ).then(
                                literal("end").executes(TappaaCommand::endDuel)
                        )
                ).then(
                        literal("kit").then(
                                literal("ui").executes(TappaaCommand::openKitUi)
                        ).then(
                                literal("from").then(
                                        argument("player", EntityArgument.player()).executes(TappaaCommand::kitFromPlayer)
                                )
                        ).then(
                                literal("item").then(
                                        argument("slot", SlotArgument.slot()).then(
                                                argument("item", ItemArgument.item(ctx)).executes(TappaaCommand::kitSetItem).then(
                                                        argument("count", IntegerArgumentType.integer(0)).executes(TappaaCommand::kitSetItem)
                                                )
                                        )
                                )
                        ).then(
                                literal("reset").executes(TappaaCommand::kitReset)
                        ).then(
                                literal("save").then(
                                        argument("id", IdentifierArgument.id()).executes(TappaaCommand::kitSave)
                                )
                        ).then(
                                literal("load").then(
                                        argument("id", IdentifierArgument.id())
                                                .suggests(new AvailableKitsSuggestionProvider())
                                                .executes(TappaaCommand::kitLoad)
                                )
                        ).then(
                                literal("delete").then(
                                        argument("id", IdentifierArgument.id())
                                                .suggests(new AvailableKitsSuggestionProvider())
                                                .executes(TappaaCommand::kitDelete)
                                )
                        ).then(
                                literal("copyheld").then(
                                        argument("slot", SlotArgument.slot()).then(
                                                argument("from", EntityArgument.player()).executes(TappaaCommand::kitSetItemHeld)
                                        )
                                )
                        )
                )
        );


    }


    public static void playYaySound(ServerPlayer target) {
        TappaaUtil.playSoundToPlayer(target, SoundEvents.NOTE_BLOCK_BANJO.value(), 1f, 1.2f);
    }

    public static void playBadSound(ServerPlayer target) {
        TappaaUtil.playSoundToPlayer(target, SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 1.0f);
    }

    public static void playDuelSound(ServerPlayer target) {
        TappaaUtil.playSoundToPlayer(target, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 1, 1.8f);
    }

    // GROUP

    private static int createGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (GroupManager.INSTANCE.getGroup(src).isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_GROUP.create();
        }

        GroupManager.INSTANCE.createGroup(src);

        playYaySound(src);
        src.sendSystemMessage(Component.literal("Successfully created group.").withStyle(ChatFormatting.GREEN));
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

        if (group.get().isQuickDuelGroup()) {
            playBadSound(src);
            throw ERROR_CANT_INVITE_NOW.create();
        }

        List<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "players").stream().toList();

        if (targets.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_ENOUGH_TARGETS_FOR_INVITE.create();
        }

        boolean onlyOne = targets.size() == 1;

        int failures = 0;
        for (ServerPlayer target : targets) {
            if (src == target) {
                failures += 1;
                if (onlyOne) {
                    playBadSound(src);
                    throw ERROR_CANT_INVITE_SELF.create();
                }
                continue;
            }

            if (group.get().hasMember(target)) {
                failures += 1;
                if (onlyOne) {
                    playBadSound(src);
                    throw ERROR_PLAYER_ALREADY_IN_GROUP.create();
                }
                continue;
            }

            if (DuelManager.INSTANCE.getDuel(target).isPresent()) {
                failures += 1;
                if (onlyOne) {
                    playBadSound(src);
                    throw ERROR_CANT_INVITE_NOW.create();
                }
                continue;
            }

            group.get().invite(target);
            target.sendSystemMessage(ComponentMessages.target_InvitedToGroupRequest(group.get(), false));


            group.get().broadcastMessage(ComponentMessages.broadcast_InvitedToGroup(group.get(), target), false);
        }

        if (failures == targets.size()) {
            playBadSound(src);
            src.sendSystemMessage(Component.literal("Failed to send all requests.").withStyle(ChatFormatting.DARK_RED));
        } else if (targets.size() == 1) {
            playYaySound(src);
            src.sendSystemMessage(ComponentMessages.source_InvitedToGroup(targets.getFirst()));
        } else {
            playYaySound(src);
            src.sendSystemMessage(Component.literal("Sent " + (targets.size() - failures) + " requests.").withStyle(ChatFormatting.GREEN));
        }

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
        group.get().broadcastMessage(ComponentMessages.broadcast_LeftGroup(src), true);

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

        src.sendSystemMessage(ComponentMessages.source_JoinedGroup(group.get()));
        playYaySound(src);

        group.get().broadcastMessage(ComponentMessages.broadcast_JoinedGroup(src), true);
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

        src.sendSystemMessage(ComponentMessages.source_LeftGroup(group.get()));
        playBadSound(src);

        Component msg = ComponentMessages.broadcast_DeclinedInvite(src);
        group.get().forEachMember(member -> {
            member.sendSystemMessage(msg);
            playBadSound(member);
        }, true);

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

        target.sendSystemMessage(ComponentMessages.target_KickedFromGroup(group.get()));
        TappaaCommand.playBadSound(target);

        group.get().broadcastMessage(ComponentMessages.broadcast_KickedFromGroup(target), true);
        group.get().forEachMember(TappaaCommand::playBadSound, true);

        return 1;
    }

    // DUEL

    @Deprecated
    private static int duelDirect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_GROUP.create();
        }

        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(src);
        if (duel.isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_DUEL.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        Optional<Group> targetGroup = GroupManager.INSTANCE.getGroup(target);
        if (targetGroup.isPresent()) {
            playBadSound(src);
            throw ERROR_OTHER_IN_GROUP.create();
        }

        Optional<Duel> targetDuel = DuelManager.INSTANCE.getDuel(target);
        if (targetDuel.isPresent()) {
            playBadSound(src);
            throw ERROR_OTHER_IN_DUEL.create();
        }

        // set up duel
        Identifier kitId;
        try {
            kitId = IdentifierArgument.getId(ctx, "kit");
        } catch (IllegalArgumentException iae) {
            kitId = null;
        }

        Group duelGroup = GroupManager.INSTANCE.createQuickDuelGroup(ctx.getSource().getServer(), src, kitId);

        if (duelGroup == null) {
            playBadSound(src);
            throw ERROR_KIT_DOESNT_EXIST.create();
        } else {

            duelGroup.invite(target);
            target.sendSystemMessage(ComponentMessages.target_InvitedToGroupRequest(duelGroup, true));

            playYaySound(src);
            src.sendSystemMessage(ComponentMessages.source_SentQuickDuelRequestSingle(target));

            return 1;
        }
    }

    private static int duelDirectMultiple(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_GROUP.create();
        }

        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(src);
        if (duel.isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_DUEL.create();
        }

        // set up duel
        Identifier kitId;
        try {
            kitId = IdentifierArgument.getId(ctx, "kit");
        } catch (IllegalArgumentException iae) {
            kitId = null;
        }

        Group duelGroup = GroupManager.INSTANCE.createQuickDuelGroup(ctx.getSource().getServer(), src, kitId);

        if (duelGroup == null) {
            playBadSound(src);
            throw ERROR_KIT_DOESNT_EXIST.create();
        }

        List<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "players").stream().toList();

        if (targets.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_ENOUGH_TARGETS_FOR_INVITE.create();
        }

        boolean onlyOne = targets.size() == 1;

        int failures = 0;
        for (ServerPlayer target : targets) {
            Optional<Group> targetGroup = GroupManager.INSTANCE.getGroup(target);
            if (targetGroup.isPresent()) {
                failures += 1;
                if (onlyOne) {
                    playBadSound(src);
                    throw ERROR_OTHER_IN_GROUP.create();
                }
                continue;
            }

            Optional<Duel> targetDuel = DuelManager.INSTANCE.getDuel(target);
            if (targetDuel.isPresent()) {
                failures += 1;
                if (onlyOne) {
                    playBadSound(src);
                    throw ERROR_OTHER_IN_DUEL.create();
                }
                continue;
            }

            duelGroup.invite(target);
            target.sendSystemMessage(ComponentMessages.target_InvitedToGroupRequest(duelGroup, true));
        }


        if (failures == targets.size()) {
            playBadSound(src);
            src.sendSystemMessage(Component.literal("Failed to send all requests.").withStyle(ChatFormatting.DARK_RED));
        } else if (targets.size() == 1) {
            playYaySound(src);
            src.sendSystemMessage(ComponentMessages.source_SentQuickDuelRequestSingle(targets.getFirst()));
        } else {
            playYaySound(src);
            src.sendSystemMessage(Component.literal("Sent " + (targets.size() - failures) + " requests.").withStyle(ChatFormatting.GREEN));
        }

        return 1;
    }

    private static int duelGroup(CommandContext<CommandSourceStack> ctx, boolean useKit) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_DUEL.create();
        }

        if (group.get().getMembers().size() <= 1) {
            playBadSound(src);
            throw ERROR_NOT_ENOUGH_PLAYERS.create();
        }

        Optional<Duel> duel = DuelManager.INSTANCE.createDuelFromGroup(group.get(), false, useKit);

        if (duel.isPresent()) { // this should always be true, really
            for (ServerPlayer member : duel.get().getPlayers()) {
                playDuelSound(member);
            }
        }

        return 1;
    }

    private static int endDuel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(src);

        if (duel.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_DUEL.create();
        }

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            playBadSound(src);
            throw ERROR_NOT_GROUP_OWNER.create();
        }


        duel.get().endDuel(true);

        return 1;
    }


    // KIT

    private static int openKitUi(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (!(src.getPermissionContext().permissionLevel().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS) || src.gameMode() == GameType.CREATIVE)) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_NO_PERMISSION.create();
        }

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            playBadSound(src);
            throw ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        group.get().getKitManager().openContainerToPlayer(src);

        return 1;
    }

    private static int kitFromPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        group.get().getKitManager().fromPlayerInventory(target);

        playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Kit updated to ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(target,ChatFormatting.GOLD))
                .append(Component.literal("'s inventory.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSetItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        int slot = SlotArgument.getSlot(ctx, "slot");
        ItemInput item = ItemArgument.getItem(ctx, "item");

        int count;
        try {
            count = IntegerArgumentType.getInteger(ctx, "count");
        } catch (IllegalArgumentException _) {
            count = 1;
        }

        ItemStack stack = item.createItemStack(count);

        int realSlot = Kit.slotMapPlayer2Kit(slot);

        if (realSlot == -1) {
            playBadSound(src);
            throw ERROR_BAD_SLOT.create();
        }

        group.get().getKitManager().setItem(realSlot, stack);
        
        playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Added ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.itemStackComponent(stack)) // item
                .append(Component.literal(" to kit.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSetItemHeld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        int slot = SlotArgument.getSlot(ctx, "slot");
        ServerPlayer target = EntityArgument.getPlayer(ctx, "from");

        ItemStack stack = target.getMainHandItem();

        int realSlot = Kit.slotMapPlayer2Kit(slot);

        if (realSlot == -1) {
            playBadSound(src);
            throw ERROR_BAD_SLOT.create();
        }

        group.get().getKitManager().setItem(realSlot, stack);

        playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Added ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.itemStackComponent(stack)) // item
                .append(Component.literal(" to kit.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }


        group.get().getKitManager().resetKitToDefault();

        playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Reset kit.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        Identifier id = IdentifierArgument.getId(ctx, "id");

        SavedKits.getSavedKits(ctx.getSource().getServer())
                .saveKit(id, group.get().getKitManager().getKit().mapped(), true);


        playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Saved kit as [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitLoad(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        Identifier id = IdentifierArgument.getId(ctx, "id");

        SavedKits.getSavedKits(ctx.getSource().getServer()).getMappedItemsFromId(id).ifPresentOrElse(map -> {
            group.get().getKitManager().setKit(Kit.fromMap(map));

            playYaySound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("Loaded kit [").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
            );
        }, () -> {
            playBadSound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("No kit [").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("] exists!").withStyle(ChatFormatting.DARK_RED))
            );
        });

        return 1;
    }

    private static int kitDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            playBadSound(src);
            throw ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        Identifier id = IdentifierArgument.getId(ctx, "id");

        boolean success = SavedKits.getSavedKits(ctx.getSource().getServer()).remove(id);

        if (success) {
            playYaySound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("Deleted kit [").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
            );
        } else {
            playBadSound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("No such kit [").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("] exists to delete!").withStyle(ChatFormatting.DARK_RED))
            );
        }


        return 1;
    }


    // DEV

    private static int devForceCreateGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (GroupManager.INSTANCE.getGroup(src).isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_GROUP.create();
        }

        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");

        // remove from groups, disband if theyre the owner (ill make an owner-swap thing later)
        for (ServerPlayer player : players) {
            if (player == src) {
                continue;
            }

            Optional<Group> group = GroupManager.INSTANCE.getGroup(player);
            if (group.isPresent()) {
                if (group.get().getOwner() == player) {
                    GroupManager.INSTANCE.disbandGroup(group.get(), false);
                } else {
                    group.get().removeFromGroup(player);

                    player.sendSystemMessage(ComponentMessages.target_KickedFromGroup(group.get()));
                    TappaaCommand.playBadSound(player);

                    group.get().broadcastMessage(ComponentMessages.broadcast_KickedFromGroup(player), true);
                    group.get().forEachMember(TappaaCommand::playBadSound, true);
                }
            }
        }

        Group newGroup = GroupManager.INSTANCE.createGroup(src);

        players.forEach(player -> {
            newGroup.addToGroup(player);
            TappaaCommand.playYaySound(player);
            player.sendSystemMessage(Component.empty()
                    .append(Component.literal("You have been forcefully added to ").withStyle(ChatFormatting.GREEN))
                    .append(TappaaUtil.playerTextComponent(src, ChatFormatting.GOLD))
                    .append(Component.literal("'s group.").withStyle(ChatFormatting.GREEN))

            );
        });

        playYaySound(src);
        src.sendSystemMessage(Component.literal("Succesfully force-created group.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int devForceInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_NOT_IN_GROUP.create();
        }

        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");

        for (ServerPlayer player : players) {
            if (player == src) {
                continue;
            }

            GroupManager.INSTANCE.getGroup(player).ifPresent(playerGroup -> {
                if (playerGroup.getOwner() == player) {
                    GroupManager.INSTANCE.disbandGroup(group.get(), false);
                } else {
                    playerGroup.removeFromGroup(player);

                    player.sendSystemMessage(ComponentMessages.target_KickedFromGroup(group.get()));
                    TappaaCommand.playBadSound(player);

                    playerGroup.broadcastMessage(ComponentMessages.broadcast_KickedFromGroup(player), true);
                    playerGroup.forEachMember(TappaaCommand::playBadSound, true);

                }
            });
        }

        players.forEach(player -> {
            group.get().addToGroup(player);
            TappaaCommand.playYaySound(player);
            player.sendSystemMessage(Component.empty()
                    .append(Component.literal("You have been forcefully added to ").withStyle(ChatFormatting.GREEN))
                    .append(TappaaUtil.playerTextComponent(src, ChatFormatting.GOLD))
                    .append(Component.literal("'s group.").withStyle(ChatFormatting.GREEN))

            );
        });

        playYaySound(src);
        src.sendSystemMessage(Component.literal("Succesfully force-invited player(s).").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int devForceDuelDirect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_GROUP.create();
        }

        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(src);
        if (duel.isPresent()) {
            playBadSound(src);
            throw ERROR_ALREADY_IN_DUEL.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        Optional<Group> targetGroup = GroupManager.INSTANCE.getGroup(target);
        if (targetGroup.isPresent()) {
            playBadSound(src);
            throw ERROR_OTHER_IN_GROUP.create();
        }

        Optional<Duel> targetDuel = DuelManager.INSTANCE.getDuel(target);
        if (targetDuel.isPresent()) {
            playBadSound(src);
            throw ERROR_OTHER_IN_DUEL.create();
        }

        // set up duel
        boolean useKit;
        try {
            useKit = BoolArgumentType.getBool(ctx, "use_kit");
        } catch (IllegalArgumentException iae) {
            useKit = true;
        }

        target.sendSystemMessage(Component.empty()
                .append(Component.literal("You are forcefully dueling ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(src, ChatFormatting.GOLD))
        );

        Group duelGroup = GroupManager.INSTANCE.createGroup(src);
        duelGroup.addToGroup(target);
        DuelManager.INSTANCE.createDuelFromGroup(duelGroup, true, useKit);

        return 1;
    }

    private static int devGetGroupUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        Optional<Group> group = GroupManager.INSTANCE.getGroup(target);
        if (group.isEmpty()) {
            playBadSound(src);
            throw ERROR_OTHER_NOT_IN_GROUP.create();
        }

        src.sendSystemMessage(Component.empty()
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" is a part of ").withStyle(ChatFormatting.WHITE))
                .append(TappaaUtil.playerTextComponent(group.get().getOwner(), ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GOLD))
                .append(Component.literal("'s group").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GOLD))
                .append(Component.literal(" with UUID ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("[").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(group.get().getGroupUuid().toString())
                        .withStyle(ChatFormatting.AQUA)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(group.get().getGroupUuid().toString())))
                        .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))))
                )
                .append(Component.literal("]").withStyle(ChatFormatting.GREEN))
        );
        return 1;
    }

    private static int devGetDuelUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");


        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(target);
        if (duel.isEmpty()) {
            playBadSound(src);
            throw ERROR_OTHER_NOT_IN_DUEL.create();
        }

        src.sendSystemMessage(Component.empty()
                .append(TappaaUtil.playerTextComponent(target, ChatFormatting.GOLD))
                .append(Component.literal(" is a part of a duel with UUID: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("[").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(duel.get().getUuid().toString())
                        .withStyle(ChatFormatting.AQUA)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(duel.get().getUuid().toString())))
                )
                .append(Component.literal("]").withStyle(ChatFormatting.GREEN))
        );
        return 1;
    }

}
