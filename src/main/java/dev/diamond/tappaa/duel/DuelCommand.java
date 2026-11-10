package dev.diamond.tappaa.duel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.diamond.tappaa.Tappaa;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class DuelCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {

        dispatcher.register(
                literal("duel").then(
                        literal("challenge").then(
                                argument("player", EntityArgument.player())
                                        .executes(DuelCommand::challengePlayer)
                        )
                ).then(
                        literal("accept").then(
                                argument("player", EntityArgument.player())
                                        .executes(DuelCommand::acceptDuel)
                        )
                )
        );


    }



    private static int challengePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        Component message = Duel.getProposeDuelMessage(self);
        player.sendSystemMessage(message);

        return 1;
    }

    private static int acceptDuel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        ServerPlayer requestee = EntityArgument.getPlayer(ctx, "player");

        Component message = Duel.getAcceptedDuelMessage(requestee);
        self.sendSystemMessage(message);

        return 1;
    }

}
