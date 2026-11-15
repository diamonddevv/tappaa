package dev.diamond.tappaa;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.component.ResolvableProfile;

import java.time.Duration;

public class TappaaUtil {

    public static void playSoundToPlayer(ServerPlayer target, SoundEvent sound, float volume, float pitch) {
        target.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound),
                SoundSource.MASTER,
                target.getX(), target.getY(), target.getZ(),
                volume, pitch, target.getRandom().nextLong()
        ));
    }

    public static void setPlayerActionbarText(ServerPlayer target, Component component) {
        target.connection.send(new ClientboundSetActionBarTextPacket(component));
    }

    public static MutableComponent playerTextComponent(ServerPlayer player, int ljustName, ChatFormatting... formats) {
        ResolvableProfile profile = player.getProfile();
        return (Component.empty()
                .append(Component.object(new PlayerSprite(profile, true)))
                .append(Component.literal(" "))
                .append(Component.literal(ljust(profile.partialProfile().name(), ljustName)).withStyle(formats))
        ).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(EntityTypes.PLAYER, player.getUUID(), player.getName()))));
    }

    public static MutableComponent playerTextComponent(ServerPlayer player, ChatFormatting... formats) {
        return playerTextComponent(player, -1, formats);
    }

    public static String ljust(String string, int size) {
        StringBuilder s = new StringBuilder();
        s.append(string);
        while (s.length() < size){
            s.append(" ");
        }
        return s.toString();
    }

    public static String ticksToMinsSecs(int ticks) {
        int seconds = (int) Math.floor(ticks / 20f);

        int secondsLeft = seconds % 60;
        int minutes = seconds / 60;

        String MM = ((minutes     < 10) ? "0" : "") + minutes;
        String SS = ((secondsLeft < 10) ? "0" : "") + secondsLeft;

        return MM + ":" + SS;
    }
}
