package dev.diamond.tappaa;

import dev.diamond.tappaa.duel.DuelCommand;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tappaa implements ModInitializer {
	public static final String MOD_ID = "tappaa";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		CommandRegistrationCallback.EVENT.register(DuelCommand::register);

	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static MutableComponent playerTextComponent(ServerPlayer player, ChatFormatting... formats) {
		ResolvableProfile profile = player.getProfile();
		return Component.empty()
				.append(Component.object(new PlayerSprite(profile, true)))
				.append(Component.literal(" "))
				.append(Component.literal(profile.partialProfile().name()).withStyle(formats));
	}
}
