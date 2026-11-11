package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class DuelRequest {
    private final ServerPlayer requestee;
    private final Collection<ServerPlayer> targets;
    private final HashMap<ServerPlayer, Boolean> accepted;
    private boolean cancelled;

    public DuelRequest(ServerPlayer requestee, Collection<ServerPlayer> targets) {
        this.requestee = requestee;
        this.targets = targets;
        this.cancelled = false;
        this.accepted = new HashMap<>();

        for (ServerPlayer target : targets) {
            this.accepted.put(target, false);
        }
    }

    public ServerPlayer getRequestee() {
        return requestee;
    }

    public UUID getRequesteeUuid() {
        return requestee.getUUID();
    }

    public List<UUID> getTargetUuids() {
        return targets.stream().map(ServerPlayer::getUUID).toList();
    }

    public boolean isRequestee(ServerPlayer player) {
        return player == requestee;
    }

    public boolean targetsPlayer(ServerPlayer player) {
        return this.targets.contains(player);
    }

    public boolean cancelled() {
        return this.cancelled;
    }

    public boolean playerHasAccepted(ServerPlayer player) {
        return this.accepted.get(player);
    }

    public boolean allPlayersAccepted() {
        return this.accepted.values().stream().allMatch(b -> b);
    }

    public List<ServerPlayer> acceptedPlayers() {
        List<ServerPlayer> acceptedPlayers = new ArrayList<>();
        for (ServerPlayer player : this.accepted.keySet()) {
            if (this.accepted.get(player)) {
                acceptedPlayers.add(player);
            }
        }
        return acceptedPlayers;
    }

    public List<ServerPlayer> involvedPlayers() {
        List<ServerPlayer> players = acceptedPlayers();
        players.add(requestee);
        return players;
    }

    public void markCancelled() {
        this.cancelled = true;
    }

    public void markAccepted(ServerPlayer player) {
        accepted.put(player, true);
    }

    public Component getSentRequestMessage() {
        return Component.empty()
                .append(Component.literal("Sent request to " + targets.size() + " player(s).").withStyle(ChatFormatting.GREEN)
                );
    }

    public Component getProposeMessage() {

        String acceptCommand = "duel accept " + requestee.getPlainTextName();
        String declineCommand = "duel decline " + requestee.getPlainTextName();

        return Component.empty()
                .append(Tappaa.playerTextComponent(requestee, ChatFormatting.GOLD))
                .append(Component.literal(" has requested a duel. ").withColor(TextColor.GREEN))
                .append(Component.literal("[ACCEPT]")
                        .withColor(TextColor.LIGHT_PURPLE)
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(acceptCommand))))
                .append(" ")
                .append(Component.literal("[DECLINE]")
                        .withColor(TextColor.DARK_RED)
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(declineCommand))))
                ;
    }

    public Component getAcceptedMessage(ServerPlayer player) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(player, ChatFormatting.GOLD))
                .append(Component.literal(" joined duel. ").withColor(TextColor.GREEN))
                .append(Component.literal("[" + (acceptedPlayers().size() + 1) + "/" + targets.size() + "]").withColor(TextColor.YELLOW))
                ;
    }

    public Component getAcceptMessage() {
        return Component.empty()
                .append(Component.literal("Accepted ").withColor(TextColor.GREEN))
                .append(Tappaa.playerTextComponent(requestee, ChatFormatting.GOLD))
                .append(Component.literal("'s duel.").withColor(TextColor.GREEN))
                ;
    }

    public Component getDeclineMessage() {
        return Component.empty()
                .append(Component.literal("Declined ").withColor(TextColor.GREEN))
                .append(Tappaa.playerTextComponent(requestee, ChatFormatting.GOLD))
                .append(Component.literal("'s duel.").withColor(TextColor.GREEN))
                ;
    }

    public Component getCancelledMessage() {
        return Component.empty()
                .append(Component.literal("Duel request cancelled.").withStyle(ChatFormatting.RED));
    }

    public Component getHostCancelledMessage(DuelRequest request) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(request.requestee))
                .append(Component.literal(" cancelled the duel.").withStyle(ChatFormatting.RED));
    }

    public Component getPlayerDeclinedMessage(ServerPlayer player) {
        return Component.empty()
                .append(Tappaa.playerTextComponent(player))
                .append(Component.literal(" declined the duel.").withStyle(ChatFormatting.RED));
    }



}
