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

    private static final Codec<SavedKits> CODEC = SavedKitData.CODEC.listOf().xmap(
            SavedKits::new,
            SavedKits::getRawData
    );

    private static final SavedDataType<SavedKits> TYPE = new SavedDataType<>(
            Tappaa.id("saved_kits"),
            SavedKits::new,
            CODEC,
            null
    );

    private final HashMap<Identifier, SavedKitData> data;

    private SavedKits() {
        this.data = new HashMap<>();
    }

    private SavedKits(List<SavedKitData> list) {
        this.data = new HashMap<>();

        for (SavedKitData kit : list) {
            this.data.put(kit.id, kit);
        }
    }

    private List<SavedKitData> getRawData() {
        return data.values().stream().toList();
    }


    public Collection<String> getIdStrings() {
        return this.data.keySet().stream().map(Identifier::toString).toList();
    }

    public Optional<HashMap<Integer, ItemStack>> getMappedItemsFromId(Identifier id) {
        return Optional.ofNullable(this.data.get(id)).map(SavedKitData::mapItems);
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

        this.data.put(id, new SavedKitData(id, stacks));

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

    private record SavedKitData(Identifier id, List<IndexedItemStack> items) {
        public static final Codec<SavedKitData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("id").forGetter(SavedKitData::id),
                IndexedItemStack.CODEC.listOf().fieldOf("items").forGetter(SavedKitData::items)
        ).apply(instance, SavedKitData::new));

        public HashMap<Integer, ItemStack> mapItems() {
            HashMap<Integer, ItemStack> map = new HashMap<>();
            for (IndexedItemStack idxStack : items) {
                map.put(idxStack.idx, idxStack.stack);
            }
            return map;
        }
    }

    private record IndexedItemStack(int idx, ItemStack stack) {
        public static final Codec<IndexedItemStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("idx").forGetter(IndexedItemStack::idx),
                ItemStack.CODEC.fieldOf("stack").forGetter(IndexedItemStack::stack)
        ).apply(instance, IndexedItemStack::new));
    }
}
