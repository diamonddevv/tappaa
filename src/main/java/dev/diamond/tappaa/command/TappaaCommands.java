package dev.diamond.tappaa.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class TappaaCommands {
    public static void registerAll() {
        CommandRegistrationCallback.EVENT.register(TappaaGeneralCommand::register);

        CommandRegistrationCallback.EVENT.register(GroupCommand::register);
        CommandRegistrationCallback.EVENT.register(DuelCommand::register);
        CommandRegistrationCallback.EVENT.register(KitCommand::register);
    }
}
