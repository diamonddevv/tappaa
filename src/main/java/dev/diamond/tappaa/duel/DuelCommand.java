package dev.diamond.tappaa.duel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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

    public static final DynamicCommandExceptionType ERROR_DUEL_NOT_FOUND = new DynamicCommandExceptionType(player -> Component.literal("No duel against " + Tappaa.playerTextComponent(((ServerPlayer)player)) + " was found"));
    public static final SimpleCommandExceptionType ERROR_NO_DUEL_REQUEST = new SimpleCommandExceptionType(Component.literal("You have no outgoing duel request!"));

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
                ).then(
                        literal("cancel")
                                .executes(DuelCommand::cancelRequest)
                )
        );


    }



    private static int challengePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        DuelRequest request = DuelManager.INSTANCE.requestDuel(self, player);

        Component message = request.getProposeMessage();
        player.sendSystemMessage(message);

        return 1;
    }

    private static int acceptDuel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        ServerPlayer requestee = EntityArgument.getPlayer(ctx, "player");

        if (DuelManager.INSTANCE.checkValidRequestExists(requestee, self)) {
            DuelRequest request = DuelManager.INSTANCE.getRequestedDuel(requestee);
            Component message = request.getAcceptMessage();
            self.sendSystemMessage(message);
            return 1;
        } else {
            throw ERROR_DUEL_NOT_FOUND.create(requestee);
        }
    }

    private static int cancelRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        if (DuelManager.INSTANCE.playerHasActiveRequest(self)) {
            DuelRequest request = DuelManager.INSTANCE.getRequestedDuel(self);
            Component message = request.getCancelledMessage();
            self.sendSystemMessage(message);
            request.cancelRequest();
            return 1;
        } else {
            throw ERROR_NO_DUEL_REQUEST.create();
        }
    }

}
