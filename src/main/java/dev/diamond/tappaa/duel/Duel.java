package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

public class Duel {

    public Duel() {

    }

    public static Component getProposeDuelMessage(ServerPlayer requestee) {

        String acceptCommand = "duel accept " + requestee.getPlainTextName();

        return Component.empty()
                .append(Tappaa.playerTextComponent(requestee, ChatFormatting.GOLD))
                .append(Component.literal(" has requested a duel. ").withColor(TextColor.GREEN))
                .append(Component.literal("[ACCEPT]")
                        .withColor(TextColor.LIGHT_PURPLE)
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(acceptCommand)))
                );
    }

    public static Component getAcceptedDuelMessage(ServerPlayer requestee) {


        return Component.empty()
                .append(Component.literal("Accepted ").withColor(TextColor.GREEN))
                .append(Tappaa.playerTextComponent(requestee, ChatFormatting.GOLD))
                .append(Component.literal("'s duel.").withColor(TextColor.GREEN))
                ;
    }
}
