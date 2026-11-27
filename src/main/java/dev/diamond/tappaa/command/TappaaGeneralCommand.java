package dev.diamond.tappaa.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.duel.*;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupManager;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.GameType;

import java.util.Collection;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class TappaaGeneralCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {

        dispatcher.register(
                literal("tappaa").executes(TappaaGeneralCommand::version).then(
                        literal("dev").requires(src -> FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment() || src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))).then(
                                literal("forcegroup").then(
                                        argument("players", EntityArgument.players()).executes(TappaaGeneralCommand::devForceCreateGroup)
                                )
                        ).then(
                                literal("uuid").then(
                                        argument("player", EntityArgument.player()).then(
                                                literal("group").executes(TappaaGeneralCommand::devGetGroupUuid)
                                        ).then(
                                                literal("duel").executes(TappaaGeneralCommand::devGetDuelUuid)
                                        )
                                )
                        ).then(
                                literal("forceinvite").then(
                                        argument("players", EntityArgument.players()).executes(TappaaGeneralCommand::devForceInvite)
                                )
                        ).then(
                                literal("forceduel").then(
                                        argument("player", EntityArgument.player()).executes(TappaaGeneralCommand::devForceDuelDirect)
                                )
                        ).then(
                                literal("kitui").executes(TappaaGeneralCommand::devOpenKitUi)
                        )
                )
        );
    }

    private static int version(CommandContext<CommandSourceStack> ctx) {

        ModContainer mod = FabricLoaderImpl.INSTANCE.getModContainer(Tappaa.MOD_ID).orElseThrow();

        ctx.getSource().sendSystemMessage(Component.empty()
                .append(Component.literal("Server is running: ").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                .append(Component.literal("tappaa").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(" ")
                .append(Component.empty()
                        .append("v")
                        .append(Component.literal(mod.getMetadata().getVersion().getFriendlyString()))
                        .append(FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment() ? "-indev" : "")
                ).withStyle(ChatFormatting.AQUA)
        );

        return 1;
    }


    // DEV
    private static int devForceCreateGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (GroupManager.INSTANCE.getGroup(src).isPresent()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_ALREADY_IN_GROUP.create();
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

                    player.sendSystemMessage(PlayerFeedback.target_KickedFromGroup(group.get()));
                    PlayerFeedback.playBadSound(player);

                    group.get().broadcastMessage(PlayerFeedback.broadcast_KickedFromGroup(player), true);
                    group.get().forEachMember(PlayerFeedback::playBadSound, true);
                }
            }
        }

        Group newGroup = GroupManager.INSTANCE.createGroup(src);

        players.forEach(player -> {
            newGroup.addToGroup(player);
            PlayerFeedback.playYaySound(player);
            player.sendSystemMessage(Component.empty()
                    .append(Component.literal("You have been forcefully added to ").withStyle(ChatFormatting.GREEN))
                    .append(TappaaUtil.playerTextComponent(src, ChatFormatting.GOLD))
                    .append(Component.literal("'s group.").withStyle(ChatFormatting.GREEN))

            );
        });

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.literal("Succesfully force-created group.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int devForceInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
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

                    player.sendSystemMessage(PlayerFeedback.target_KickedFromGroup(group.get()));
                    PlayerFeedback.playBadSound(player);

                    playerGroup.broadcastMessage(PlayerFeedback.broadcast_KickedFromGroup(player), true);
                    playerGroup.forEachMember(PlayerFeedback::playBadSound, true);

                }
            });
        }

        players.forEach(player -> {
            group.get().addToGroup(player);
            PlayerFeedback.playYaySound(player);
            player.sendSystemMessage(Component.empty()
                    .append(Component.literal("You have been forcefully added to ").withStyle(ChatFormatting.GREEN))
                    .append(TappaaUtil.playerTextComponent(src, ChatFormatting.GOLD))
                    .append(Component.literal("'s group.").withStyle(ChatFormatting.GREEN))

            );
        });

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.literal("Succesfully force-invited player(s).").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int devForceDuelDirect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isPresent()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_ALREADY_IN_GROUP.create();
        }

        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(src);
        if (duel.isPresent()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_ALREADY_IN_DUEL.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        Optional<Group> targetGroup = GroupManager.INSTANCE.getGroup(target);
        if (targetGroup.isPresent()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_OTHER_IN_GROUP.create();
        }

        Optional<Duel> targetDuel = DuelManager.INSTANCE.getDuel(target);
        if (targetDuel.isPresent()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_OTHER_IN_DUEL.create();
        }


        target.sendSystemMessage(Component.empty()
                .append(Component.literal("You are forcefully dueling ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(src, ChatFormatting.GOLD))
        );

        Group duelGroup = GroupManager.INSTANCE.createGroup(src);
        duelGroup.addToGroup(target);
        DuelManager.INSTANCE.createDuelFromGroup(duelGroup, true);

        return 1;
    }

    private static int devGetGroupUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        Optional<Group> group = GroupManager.INSTANCE.getGroup(target);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_OTHER_NOT_IN_GROUP.create();
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
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_OTHER_NOT_IN_DUEL.create();
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

    private static int devOpenKitUi(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        if (!(src.getPermissionContext().permissionLevel().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS) || src.gameMode() == GameType.CREATIVE)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_NO_PERMISSION.create();
        }

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        throw PlayerFeedback.ERROR_FUCK_YOU.create();

        //return 1;
    }

}
