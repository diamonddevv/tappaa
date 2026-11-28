package dev.diamond.tappaa.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.diamond.tappaa.duel.AvailableKitsSuggestionProvider;
import dev.diamond.tappaa.duel.Duel;
import dev.diamond.tappaa.duel.DuelManager;
import dev.diamond.tappaa.duel.Kit;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupManager;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DuelCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {
        dispatcher.register(
                literal("duel").then(
                        literal("request").then(
                                argument("players", EntityArgument.players()).then(
                                        KitCommand.kitArgument().executes(DuelCommand::duelDirectMultiple)
                                )
                        )
                ).then(
                        literal("group").then(
                                KitCommand.kitArgument().executes(DuelCommand::duelGroup)
                        )
                ).then(
                        literal("end").executes(DuelCommand::endDuel)
                )
        );
    }



    private static int duelDirectMultiple(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

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

        // get kit
        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        Optional<Kit> kit = SavedKits.getSavedKits(ctx.getSource().getServer()).getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        List<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "players").stream().toList();

        if (targets.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_ENOUGH_TARGETS_FOR_INVITE.create();
        }


        boolean onlyOne = targets.size() == 1;

        if (onlyOne && targets.getFirst() == src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_DUEL_SELF.create();
        }

        Group duelGroup = GroupManager.INSTANCE.createQuickDuelGroup(src, kit.get());

        int failures = 0;
        for (ServerPlayer target : targets) {
            Optional<Group> targetGroup = GroupManager.INSTANCE.getGroup(target);
            if (targetGroup.isPresent()) {
                failures += 1;
                if (onlyOne) {
                    PlayerFeedback.playBadSound(src);
                    throw PlayerFeedback.ERROR_OTHER_IN_GROUP.create();
                }
                continue;
            }

            Optional<Duel> targetDuel = DuelManager.INSTANCE.getDuel(target);
            if (targetDuel.isPresent()) {
                failures += 1;
                if (onlyOne) {
                    PlayerFeedback.playBadSound(src);
                    throw PlayerFeedback.ERROR_OTHER_IN_DUEL.create();
                }
                continue;
            }

            duelGroup.invite(target);
            target.sendSystemMessage(PlayerFeedback.target_InvitedToGroupRequest(duelGroup, true));
        }


        if (failures == targets.size()) {
            PlayerFeedback.playBadSound(src);
            src.sendSystemMessage(Component.literal("Failed to send all requests.").withStyle(ChatFormatting.DARK_RED));
        } else if (targets.size() == 1) {
            PlayerFeedback.playYaySound(src);
            src.sendSystemMessage(PlayerFeedback.source_SentQuickDuelRequestSingle(targets.getFirst()));
        } else {
            PlayerFeedback.playYaySound(src);
            src.sendSystemMessage(Component.literal("Sent " + (targets.size() - failures) + " requests.").withStyle(ChatFormatting.GREEN));
        }

        return 1;
    }

    private static int duelGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_ALREADY_IN_DUEL.create();
        }

        if (group.get().getMembers().size() <= 1) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_ENOUGH_PLAYERS.create();
        }

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        Optional<Kit> kit = SavedKits.getSavedKits(ctx.getSource().getServer()).getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        Optional<Duel> duel = DuelManager.INSTANCE.createDuelFromGroup(group.get(), kit.get(), false);

        if (duel.isPresent()) { // this should always be true, really
            for (ServerPlayer member : duel.get().getPlayers()) {
                PlayerFeedback.playDuelSound(member);
            }
        }

        return 1;
    }

    private static int endDuel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Duel> duel = DuelManager.INSTANCE.getDuel(src);

        if (duel.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_DUEL.create();
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


        duel.get().endDuel(true);

        return 1;
    }

}
