package dev.diamond.tappaa.group;

import dev.diamond.tappaa.ComponentMessages;
import dev.diamond.tappaa.TappaaCommand;
import dev.diamond.tappaa.TappaaUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class GroupRequest {

    public static final int REQUEST_TIMEOUT_DURATION = 30 * 20;

    private final Group group;
    private final UUID targetPlayerUuid;
    private int timeout;

    public GroupRequest(Group group, UUID targetPlayerUuid, int timeout) {
        this.group = group;
        this.targetPlayerUuid = targetPlayerUuid;
        this.timeout = timeout;
    }

    public ServerPlayer getTargetPlayer() {
        return group.getServer().getPlayerList().getPlayer(this.targetPlayerUuid);
    }

    public void tick() {
        if (this.timeout > 0) {
            this.timeout--;

            if (timeout <= 0) {
                this.onTimeout();
            }
        }
    }

    private void onTimeout() {
        ServerPlayer player = group.getServer().getPlayerList().getPlayer(targetPlayerUuid);
        if (player != null) {
            TappaaCommand.playBadSound(player); // borrow the bad sound lol
            player.sendSystemMessage(ComponentMessages.target_RequestExpired(this.group));

            group.forEachMember(member -> {
                TappaaCommand.playBadSound(member);
                member.sendSystemMessage(Component.empty()
                        .append(TappaaUtil.playerTextComponent(player, ChatFormatting.GOLD))
                        .append(Component.literal("'s invite expired.").withStyle(ChatFormatting.YELLOW))
                );
            }, true);
        }
        this.group.rescindInvite(targetPlayerUuid);
    }


}
