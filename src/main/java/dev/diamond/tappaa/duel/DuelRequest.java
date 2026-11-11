package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class DuelRequest {
    private final ServerPlayer requestee;
    private final ServerPlayer target;
    private boolean cancelled;

    public DuelRequest(ServerPlayer requestee, ServerPlayer target) {
        this.requestee = requestee;
        this.target = target;
        this.cancelled = false;
    }

    public UUID getRequesteeUuid() {
        return requestee.getUUID();
    }

    public UUID getTargetUuid() {
        return target.getUUID();
    }

    public boolean cancelled() {
        return this.cancelled;
    }

    public void cancelRequest() {
        this.cancelled = true;
    }

    public Component getProposeMessage() {

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

    public Component getAcceptMessage() {
        return Component.empty()
                .append(Component.literal("Accepted ").withColor(TextColor.GREEN))
                .append(Tappaa.playerTextComponent(requestee, ChatFormatting.GOLD))
                .append(Component.literal("'s duel.").withColor(TextColor.GREEN))
                ;
    }

    public Component getCancelledMessage() {
        return Component.empty()
                .append(Component.literal("Duel request cancelled.").withStyle(ChatFormatting.RED));
    }

}
