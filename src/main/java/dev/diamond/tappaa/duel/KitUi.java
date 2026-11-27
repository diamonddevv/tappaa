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
import org.jspecify.annotations.NonNull;

import java.util.List;

@Deprecated
public class KitUi implements MenuProvider, Container {

    /*
    im really tempted to make this a client-only feature.
    (that meaning, you need to have the mod on the client to use it.)

    the logistics of making sure the player effectively putting items in but also keeping
    them themselves, and then not actually giving them any items that they take out
    is difficult and im not sure how to tackle the problem. ill try, but regardless,
    im going to make this a much better experience if the mod is installed on the client
    (custom ui menu... etc.)

    this needs alot of work anyway, the slots matching could be well improved..

    will maybe just rewrite largely. it doesnt make alot of sense that a kit can only be
    edited from within a team...
     */

    private final Component containerName;
    private Kit kit;

    public KitUi() {
        this.containerName = Component.literal("Duel Kit");
        this.kit = Kit.defaultKit();
    }

    public void openContainerToPlayer(ServerPlayer player) {
        player.openMenu(this);
    }

//    public void fromPlayerInventory(ServerPlayer player) {
//        this.kit = new Kit();
//
//        for (int i = 0; i < Kit.HOTBAR_SLOTS; i++) {
//            ItemStack stack = player.getInventory().getItem(i).copy();
//            this.setItem(Kit.HOTBAR_SLOTS_START + i, stack);
//        }
//
//        for (int i = 0; i < Kit.INVENTORY_SLOTS; i++) {
//            ItemStack stack = player.getInventory().getItem(i + Kit.HOTBAR_SLOTS).copy();
//            this.setItem(i, stack);
//        }
//
//        kit.withHelmet(player.getInventory().getItem(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE)));
//        kit.withChestplate(player.getInventory().getItem(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE)));
//        kit.withLeggings(player.getInventory().getItem(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE)));
//        kit.withBoots(player.getInventory().getItem(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE)));
//        kit.withOffhand(player.getInventory().getItem(Inventory.SLOT_OFFHAND));
//
//    }


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
        return 0;// Kit.KIT_UI_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.getItems().isEmpty();
    }

    public List<ItemStack> getItems() {
        return kit.items.values().stream().toList();
    }

    public Kit getKit() {
        return kit;
    }

    public void setKit(Kit kit) {
        this.kit = kit;
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

    public void resetKitToDefault() {
        this.kit = Kit.defaultKit();
    }

}
