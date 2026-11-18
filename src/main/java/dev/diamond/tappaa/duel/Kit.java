package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.NonNull;

public class Kit implements MenuProvider, Container {
    private static final int SLOTS = 9 * 5;
    private static final int INVENTORY_SLOTS = 9 * 3;
    private static final int HOTBAR_SLOTS = 9;

    private static final int HOTBAR_SLOTS_START = 27;

    private static final int HELMET_SLOT = 36;
    private static final int CHESTPLATE_SLOT = 37;
    private static final int LEGGINGS_SLOT = 38;
    private static final int BOOTS_SLOT = 39;
    private static final int OFFHAND_SLOT = 41;

    private static final int INV_HELMET_SLOT = 103;
    private static final int INV_CHESTPLATE_SLOT = 102;
    private static final int INV_LEGGINGS_SLOT = 101;
    private static final int INV_BOOTS_SLOT = 100;
    private static final int INV_OFFHAND_SLOT = 99;

    private static final String KIT_CUSTOM_DATA_TAG = Tappaa.id("kit").toString();

    private final Component containerName;
    private final NonNullList<ItemStack> items;

    public Kit() {
        this.containerName = Component.literal("Duel Kit");
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);


        // default values
        resetToDefault();
    }

    public static int slotMapPlayer2Kit(int slot) {

        if (slot >= 0 && slot < 9) {
            return HOTBAR_SLOTS_START + slot;
        }

        if (slot >= 9 && slot < 9 + INVENTORY_SLOTS) {
            return slot - HOTBAR_SLOTS;
        }

        System.out.println(slot);

        if (slot == INV_HELMET_SLOT) return HELMET_SLOT;
        if (slot == INV_CHESTPLATE_SLOT) return CHESTPLATE_SLOT;
        if (slot == INV_LEGGINGS_SLOT) return LEGGINGS_SLOT;
        if (slot == INV_BOOTS_SLOT) return BOOTS_SLOT;
        if (slot == INV_OFFHAND_SLOT) return OFFHAND_SLOT;

        return -1;
    }

    public void openContainerToPlayer(ServerPlayer player) {
        player.openMenu(this);
    }

    public void setHelmetItem(ItemStack stack) {
        setItem(HELMET_SLOT, stack);
    }

    public void setChestplateSlot(ItemStack stack) {
        setItem(CHESTPLATE_SLOT, stack);
    }

    public void setLeggingsSlot(ItemStack stack) {
        setItem(LEGGINGS_SLOT, stack);
    }

    public void setBootsSlot(ItemStack stack) {
        setItem(BOOTS_SLOT, stack);
    }

    public void setOffhandSlot(ItemStack stack) {
        setItem(OFFHAND_SLOT, stack);
    }

    public void setHotbarSlot(ItemStack stack, int hotbarSlot) {
        if (hotbarSlot >= 0 && hotbarSlot < HOTBAR_SLOTS) {
            setItem(HOTBAR_SLOTS_START + hotbarSlot, stack);
        }
    }

    public void setInventorySlot(ItemStack stack, int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < INVENTORY_SLOTS) {
            setItem(inventorySlot, stack);
        }
    }

    public void copyToPlayer(ServerPlayer player) {

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            player.getInventory().setItem(i, this.getItemForGiving(HOTBAR_SLOTS_START + i));
        }

        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            player.getInventory().setItem(i + HOTBAR_SLOTS, this.getItemForGiving(i));
        }

        player.getInventory().setItem(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(HELMET_SLOT));
        player.getInventory().setItem(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(CHESTPLATE_SLOT));
        player.getInventory().setItem(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(LEGGINGS_SLOT));
        player.getInventory().setItem(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE), this.getItemForGiving(BOOTS_SLOT));
        player.getInventory().setItem(Inventory.SLOT_OFFHAND, this.getItemForGiving(OFFHAND_SLOT));
    }

    public static NonNullList<ItemStack> savePlayerInventory(ServerPlayer player) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            stacks.set(i, player.getInventory().getItem(i).copy());
        }

        stacks.set(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE),   player.getInventory().getItem(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE)).copy());
        stacks.set(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE),  player.getInventory().getItem(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE)).copy());
        stacks.set(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE),   player.getInventory().getItem(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE)).copy());
        stacks.set(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE),   player.getInventory().getItem(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE)).copy());
        stacks.set(Inventory.SLOT_OFFHAND,                                  player.getInventory().getItem(Inventory.SLOT_OFFHAND).copy());

        return stacks;
    }

    public static void restorePlayerInventory(ServerPlayer player, NonNullList<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            player.getInventory().setItem(i, stacks.get(i));
        };
    }

    public void fromPlayerInventory(ServerPlayer player) {
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            ItemStack stack = player.getInventory().getItem(i).copy();
            this.setItem(HOTBAR_SLOTS_START + i, stack);
        }

        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            ItemStack stack = player.getInventory().getItem(i + HOTBAR_SLOTS).copy();
            this.setItem(i, stack);
        }

        setHelmetItem(player.getInventory().getItem(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE)));
        setChestplateSlot(player.getInventory().getItem(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE)));
        setLeggingsSlot(player.getInventory().getItem(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE)));
        setBootsSlot(player.getInventory().getItem(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE)));
        setOffhandSlot(player.getInventory().getItem(Inventory.SLOT_OFFHAND));

    }

    public static void clearPlayerOfKitItems(ServerPlayer player) {
        ContainerHelper.clearOrCountMatchingItems(player.getInventory(), stack -> {
            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            return data.copyTag().getBoolean(KIT_CUSTOM_DATA_TAG).orElse(false);
        }, Integer.MAX_VALUE, false);
    }


    @Override
    public @NonNull Component getDisplayName() {
        return containerName;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @NonNull Inventory inventory, @NonNull Player player) {
        return new ChestMenu(MenuType.GENERIC_9x5, containerId, inventory, this, 5);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.getItems().isEmpty();
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }


    @Override
    public @NonNull ItemStack getItem(int slot) {
        return this.getItems().get(slot);
    }

    public @NonNull ItemStack getItemForGiving(int slot) {
        ItemStack stack = getItem(slot).copy();
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        data.update(nbt -> {
            nbt.putBoolean(KIT_CUSTOM_DATA_TAG, true);
        });
        stack.set(DataComponents.CUSTOM_DATA, data);
        return stack;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(this.getItems(), slot, count);
        if (!result.isEmpty()) {
            this.setChanged();
        }

        return result;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.getItems(), slot);
    }

    public void setItem(final int slot, final @NonNull ItemStack itemStack) {
        this.getItems().set(slot, itemStack);
        itemStack.limitSize(this.getMaxStackSize(itemStack));
        this.setChanged();
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.getItems().clear();
    }


    public void resetToDefault() {
        clearContent();

        setHelmetItem(new ItemStack(Items.IRON_HELMET));
        setChestplateSlot(new ItemStack(Items.IRON_CHESTPLATE));
        setLeggingsSlot(new ItemStack(Items.IRON_LEGGINGS));
        setBootsSlot(new ItemStack(Items.IRON_BOOTS));
        setOffhandSlot(new ItemStack(Items.SHIELD));

        setHotbarSlot(new ItemStack(Items.DIAMOND_SWORD), 0);
        setHotbarSlot(new ItemStack(Items.DIAMOND_AXE), 1);
        setHotbarSlot(new ItemStack(Items.BOW), 2);
        setHotbarSlot(new ItemStack(Items.GOLDEN_APPLE, 3), 3);
        setHotbarSlot(new ItemStack(Items.COOKED_BEEF, 64), 4);

        setInventorySlot(new ItemStack(Items.ARROW, 64), 0);
    }
}
