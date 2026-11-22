package dev.diamond.tappaa.group;

import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.TappaaCommand;
import dev.diamond.tappaa.TappaaUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class GroupRequest {
    // todo: migrate requests in groups to this class instead of direct references and test
    //       will need to change data struct to a map of uuid->request

    public static final int REQUEST_TIMEOUT_DURATION = 30 * 20;

    private final Group group;
    private final UUID targetPlayerUuid;
    private int timeout;

    public GroupRequest(Group group, UUID targetPlayerUuid, int timeout) {
        this.group = group;
        this.targetPlayerUuid = targetPlayerUuid;
        this.timeout = timeout;
    }

    public void tick() {
        this.timeout--;

        if (timeout <= 0) {
            this.onTimeout();
        }
    }

    private void onTimeout() {
        this.group.rescindInvite(targetPlayerUuid);
        ServerPlayer player = group.getServer().getPlayerList().getPlayer(targetPlayerUuid);
        if (player != null) {
            TappaaCommand.playBadSound(player); // borrow the bad sound lol
            player.sendSystemMessage(Component.empty()
                    .append(Component.literal("Your invite to ").withStyle(ChatFormatting.YELLOW))
                    .append(TappaaUtil.playerTextComponent(player, ChatFormatting.GOLD))
                    .append(Component.literal("'s " + (group.isQuickDuelGroup() ? "duel" : "group") + " has expired.'").withStyle(ChatFormatting.YELLOW))
            );

            group.forEachMember(member -> {
                TappaaCommand.playBadSound(member);
                member.sendSystemMessage(Component.empty()
                        .append(TappaaUtil.playerTextComponent(player, ChatFormatting.GOLD))
                        .append(Component.literal("'s invite expired.'").withStyle(ChatFormatting.YELLOW))
                );
            }, true);
        }
    }
}
