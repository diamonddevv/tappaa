package dev.diamond.tappaa.duel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Kit {

    public static final int KIT_UI_SLOTS = 9 * 5;
    public static final int INVENTORY_SLOTS = 9 * 3;
    public static final int HOTBAR_SLOTS = 9;
    public static final int HOTBAR_SLOTS_START = 27;

    public static final int HELMET_SLOT = 36;
    public static final int CHESTPLATE_SLOT = 37;
    public static final int LEGGINGS_SLOT = 38;
    public static final int BOOTS_SLOT = 39;
    public static final int OFFHAND_SLOT = 41;

    public static final int INV_HELMET_SLOT = 103;
    public static final int INV_CHESTPLATE_SLOT = 102;
    public static final int INV_LEGGINGS_SLOT = 101;
    public static final int INV_BOOTS_SLOT = 100;
    public static final int INV_OFFHAND_SLOT = 99;

    public static final Codec<Kit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SavedKits.IndexedItemStack.CODEC.listOf().fieldOf("stacks").forGetter(Kit::getIndexedStackList),
            Codec.INT.fieldOf("lives").forGetter(Kit::getLives),
            Codec.INT.fieldOf("respawn_ticks").forGetter(Kit::getRespawnTicks),
            Codec.BOOL.fieldOf("uses_stacks").forGetter(Kit::getUsesStacks)
    ).apply(instance, Kit::decodec));

    protected final NonNullList<ItemStack> items;

    private int lives;

    private int respawnTicks;
    private boolean usesStacks;
    public Kit() {
        this.items = NonNullList.withSize(KIT_UI_SLOTS, ItemStack.EMPTY);
        this.lives = KitManager.DEFAULT_KIT_LIVES;
        this.respawnTicks = KitManager.DEFAULT_KIT_RESPAWN_TICKS;
    }
    private static Kit decodec(List<SavedKits.IndexedItemStack> stacks, int lives, int respawnTicks, boolean usesStacks) {
        Kit kit = Kit.fromMap(SavedKits.IndexedItemStack.decomposeList(stacks));
        kit.lives = lives;
        kit.respawnTicks = respawnTicks;
        kit.usesStacks = usesStacks;
        return kit;
    }


    public Kit withHelmet(ItemStack stack) {
        setItem(HELMET_SLOT, stack);
        return this;
    }

    public Kit withChestplate(ItemStack stack) {
        setItem(CHESTPLATE_SLOT, stack);
        return this;
    }

    public Kit withLeggings(ItemStack stack) {
        setItem(LEGGINGS_SLOT, stack);
        return this;
    }

    public Kit withBoots(ItemStack stack) {
        setItem(BOOTS_SLOT, stack);
        return this;
    }

    public Kit withOffhand(ItemStack stack) {
        setItem(OFFHAND_SLOT, stack);
        return this;
    }

    public Kit withHotbar(ItemStack stack, int hotbarSlot) {
        if (hotbarSlot >= 0 && hotbarSlot < HOTBAR_SLOTS) {
            setItem(HOTBAR_SLOTS_START + hotbarSlot, stack);
        }
        return this;
    }

    public Kit withInventory(ItemStack stack, int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < INVENTORY_SLOTS) {
            setItem(inventorySlot, stack);
        }
        return this;
    }


    public static Kit fromMap(HashMap<Integer, ItemStack> items) {
        Kit kit = new Kit();

        kit.items.clear();
        for (int slot : items.keySet()) {
            kit.items.set(slot, items.get(slot));
        }

        return kit;
    }

    public HashMap<Integer, ItemStack> mapped() {
        HashMap<Integer, ItemStack> map = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack != ItemStack.EMPTY) {
                map.put(i, stack);
            }
        }
        return map;
    }

    private List<SavedKits.IndexedItemStack> getIndexedStackList() {
        List<SavedKits.IndexedItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            stacks.add(new SavedKits.IndexedItemStack(i, items.get(i)));
        }
        return stacks;
    }

    public int getLives() {
        return lives;
    }

    public int getRespawnTicks() {
        return respawnTicks;
    }

    public boolean getUsesStacks() {
        return usesStacks;
    }

    private ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    private void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
    }

    public void copyToPlayer(ServerPlayer player) {

        // todo:    change the slot indices to be the same as in the player inv
        //          and remove slot mapping function as it will be unnecessary
        //          it will break the ui but i can fix that when i want to, i
        //          dont like it right now anyway. tl;dr simplify kits

        for (int i = 0; i < Kit.HOTBAR_SLOTS; i++) {
            player.getInventory().setItem(i, this.getItemForGiving(Kit.HOTBAR_SLOTS_START + i));
        }

        for (int i = 0; i < Kit.INVENTORY_SLOTS; i++) {
            player.getInventory().setItem(i + Kit.HOTBAR_SLOTS, this.getItemForGiving(i));
        }

        player.getInventory().setItem(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(Kit.HELMET_SLOT));
        player.getInventory().setItem(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(Kit.CHESTPLATE_SLOT));
        player.getInventory().setItem(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(Kit.LEGGINGS_SLOT));
        player.getInventory().setItem(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(Kit.BOOTS_SLOT));
        player.getInventory().setItem(Inventory.SLOT_OFFHAND, this.getItemForGiving(Kit.OFFHAND_SLOT));
    }

    public @NonNull ItemStack getItemForGiving(int slot) {
        ItemStack stack = getItem(slot).copy();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(IS_OF_KIT_CUSTOM_DATA_TAG, true));
        return stack;
    }

    public static int slotMapPlayer2Kit(int slot) {

        if (slot >= 0 && slot < 9) {
            return HOTBAR_SLOTS_START + slot;
        }

        if (slot >= 9 && slot < 9 + INVENTORY_SLOTS) {
            return slot - HOTBAR_SLOTS;
        }

        if (slot == INV_HELMET_SLOT) return HELMET_SLOT;
        if (slot == INV_CHESTPLATE_SLOT) return CHESTPLATE_SLOT;
        if (slot == INV_LEGGINGS_SLOT) return LEGGINGS_SLOT;
        if (slot == INV_BOOTS_SLOT) return BOOTS_SLOT;
        if (slot == INV_OFFHAND_SLOT) return OFFHAND_SLOT;

        return -1;
    }

}
