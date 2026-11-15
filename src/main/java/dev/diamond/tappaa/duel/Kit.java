package dev.diamond.tappaa.duel;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    private final Component containerName;
    private final NonNullList<ItemStack> items;

    public Kit() {
        this.containerName = Component.literal("Duel Kit");
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);


        // default values
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
            player.getInventory().setItem(i, this.getItem(HOTBAR_SLOTS_START + i).copy());
        }

        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            player.getInventory().setItem(i + HOTBAR_SLOTS, this.getItem(i).copy());
        }

        player.getInventory().setItem(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE), this.getItem(HELMET_SLOT).copy());
        player.getInventory().setItem(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE), this.getItem(CHESTPLATE_SLOT).copy());
        player.getInventory().setItem(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE), this.getItem(LEGGINGS_SLOT).copy());
        player.getInventory().setItem(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE), this.getItem(BOOTS_SLOT).copy());
        player.getInventory().setItem(Inventory.SLOT_OFFHAND, this.getItem(OFFHAND_SLOT).copy());
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


}
