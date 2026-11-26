package dev.diamond.tappaa.serial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.duel.Kit;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class SavedKits extends SavedData {

    private static final Codec<SavedKits> CODEC = Codec.unboundedMap(
            Identifier.CODEC, Kit.CODEC
    ).xmap(
            SavedKits::new,
            SavedKits::getMap
    );

    private static final SavedDataType<SavedKits> TYPE = new SavedDataType<>(
            Tappaa.id("saved_kits"),
            SavedKits::new,
            CODEC, null
    );

    private final Map<Identifier, Kit> data;

    private SavedKits() {
        this.data = new HashMap<>();
    }

    private SavedKits(Map<Identifier, Kit> map) {
        this.data = new HashMap<>(map);
    }

    private Map<Identifier, Kit> getMap() {
        return data;
    }


    public Collection<String> getIdStrings() {
        return this.data.keySet().stream().map(Identifier::toString).toList();
    }

    public Optional<Kit> getKitFromId(Identifier id) {
        return Optional.ofNullable(this.data.get(id));
    }

    public boolean idExists(Identifier id) {
        return this.data.containsKey(id);
    }

    public boolean saveKit(Identifier id, Kit kit, boolean overwrite) {
        if (idExists(id) && !overwrite) return false;
        this.data.put(id, kit);
        this.setDirty();
        return true;
    }

    public boolean remove(Identifier id) {
        if (!idExists(id)) return false;

        this.data.remove(id);
        this.setDirty();
        return true;
    }

    public static SavedKits getSavedKits(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public record IndexedItemStack(int idx, ItemStack stack) {
        public static final Codec<IndexedItemStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("idx").forGetter(IndexedItemStack::idx),
                ItemStack.CODEC.fieldOf("stack").forGetter(IndexedItemStack::stack)
        ).apply(instance, IndexedItemStack::new));

        public static HashMap<Integer, ItemStack> decomposeList(List<IndexedItemStack> stacks) {
            HashMap<Integer, ItemStack> map = new HashMap<>();
            stacks.forEach(s -> map.put(s.idx, s.stack));
            return map;
        }
    }
}
