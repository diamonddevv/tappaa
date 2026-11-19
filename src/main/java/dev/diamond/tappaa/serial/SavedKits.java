package dev.diamond.tappaa.serial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.diamond.tappaa.Tappaa;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class SavedKits extends SavedData {

    private static final Codec<SavedKits> CODEC = Codec.unboundedMap(
            Identifier.CODEC, IndexedItemStack.CODEC.listOf()
    ).xmap(
            SavedKits::new,
            SavedKits::getMap
    );

    private static final SavedDataType<SavedKits> TYPE = new SavedDataType<>(
            Tappaa.id("saved_kits"),
            SavedKits::new,
            CODEC, null
    );

    private final Map<Identifier, List<IndexedItemStack>> data;

    private SavedKits() {
        this.data = new HashMap<>();
    }

    private SavedKits(Map<Identifier, List<IndexedItemStack>> map) {
        this.data = map;
    }

    private Map<Identifier, List<IndexedItemStack>> getMap() {
        return data;
    }


    public Collection<String> getIdStrings() {
        return this.data.keySet().stream().map(Identifier::toString).toList();
    }

    public Optional<HashMap<Integer, ItemStack>> getMappedItemsFromId(Identifier id) {
        return Optional.ofNullable(this.data.get(id)).map(IndexedItemStack::decomposeList);
    }

    public boolean idExists(Identifier id) {
        return this.data.containsKey(id);
    }

    public boolean saveKit(Identifier id, HashMap<Integer, ItemStack> mappedItems, boolean overwrite) {
        if (idExists(id) && !overwrite) return false;

        List<IndexedItemStack> stacks = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : mappedItems.entrySet()) {
            stacks.add(new IndexedItemStack(entry.getKey(), entry.getValue()));
        }

        this.data.put(id, stacks);

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


    private record IndexedItemStack(int idx, ItemStack stack) {
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
