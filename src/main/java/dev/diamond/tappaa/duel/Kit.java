package dev.diamond.tappaa.duel;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

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


    protected final NonNullList<ItemStack> items;
    private final int lives;

    public Kit() {
        this.items = NonNullList.withSize(KIT_UI_SLOTS, ItemStack.EMPTY);
        this.lives = 3;
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

    public int getLives() {
        return lives;
    }

    private ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    private void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
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
