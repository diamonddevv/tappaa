package dev.diamond.tappaa.serial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public record SavedKit(String name, UUID author, List<ItemStack> items) {
    public static final Codec<SavedKit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SavedKit::name),
            UUIDUtil.CODEC.fieldOf("author").forGetter(SavedKit::author),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(SavedKit::items)
    ).apply(instance, SavedKit::new));
}
