package dev.diamond.tappaa;

import dev.diamond.tappaa.duel.DuelManager;
import dev.diamond.tappaa.group.GroupManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tappaa implements ModInitializer {
	public static final String MOD_ID = "tappaa";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		CommandRegistrationCallback.EVENT.register(TappaaCommand::register);


		ServerPlayerEvents.JOIN.register(GroupManager::onPlayerJoinServer);
		ServerPlayerEvents.LEAVE.register(GroupManager::onPlayerLeaveServer);

		DuelManager.registerDuelingEvents();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

}
