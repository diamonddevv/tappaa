package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class KitManager implements MenuProvider, Container {

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

    private static final Identifier DEFAULT_KIT_ID = Tappaa.id("default");
    public static final int DEFAULT_KIT_LIVES = 3;
    public static final int DEFAULT_KIT_RESPAWN_TICKS = 5 * 20;

    private static final String IS_OF_KIT_CUSTOM_DATA_TAG = Tappaa.id("is_of_kit").toString();

    private final Component containerName;
    private Kit kit;

    public KitManager() {
        this.containerName = Component.literal("Duel Kit");
        this.kit = KitManager.defaultKit();
    }

    public static List<ItemStack> getCopyOfItems(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();

        for (ItemStack item : player.getInventory()) {
            items.add(item.copy());
        }

        return items;
    }

    public void openContainerToPlayer(ServerPlayer player) {
        player.openMenu(this);
    }

    public static void saveDefaultKit(MinecraftServer minecraftServer) {
        if (!SavedKits.getSavedKits(minecraftServer).idExists(DEFAULT_KIT_ID)) {
            SavedKits.getSavedKits(minecraftServer).saveKit(
                    DEFAULT_KIT_ID, KitManager.defaultKit(), true
            );
        }
    }

    public static NonNullList<ItemStack> savePlayerInventory(ServerPlayer player) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(Kit.KIT_UI_SLOTS, ItemStack.EMPTY);

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
        this.kit = new Kit();

        for (int i = 0; i < Kit.HOTBAR_SLOTS; i++) {
            ItemStack stack = player.getInventory().getItem(i).copy();
            this.setItem(Kit.HOTBAR_SLOTS_START + i, stack);
        }

        for (int i = 0; i < Kit.INVENTORY_SLOTS; i++) {
            ItemStack stack = player.getInventory().getItem(i + Kit.HOTBAR_SLOTS).copy();
            this.setItem(i, stack);
        }

        kit.withHelmet(player.getInventory().getItem(EquipmentSlot.HEAD.getIndex(Inventory.INVENTORY_SIZE)));
        kit.withChestplate(player.getInventory().getItem(EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE)));
        kit.withLeggings(player.getInventory().getItem(EquipmentSlot.LEGS.getIndex(Inventory.INVENTORY_SIZE)));
        kit.withBoots(player.getInventory().getItem(EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE)));
        kit.withOffhand(player.getInventory().getItem(Inventory.SLOT_OFFHAND));

    }

    public static void clearPlayerOfKitItems(ServerPlayer player) {
        Predicate<ItemStack> p = stack -> {
            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            Optional<Boolean> bool = data.copyTag().getBoolean(IS_OF_KIT_CUSTOM_DATA_TAG);
            return bool.orElse(false);
        };

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (p.test(stack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
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
        return Kit.KIT_UI_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.getItems().isEmpty();
    }

    public NonNullList<ItemStack> getItems() {
        return kit.items;
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
        this.kit = KitManager.defaultKit();
    }

    public static Kit defaultKit() {
        return new Kit()
                .withHelmet(new ItemStack(Items.IRON_HELMET))
                .withChestplate(new ItemStack(Items.IRON_CHESTPLATE))
                .withLeggings(new ItemStack(Items.IRON_LEGGINGS))
                .withBoots(new ItemStack(Items.IRON_BOOTS))

                .withOffhand(new ItemStack(Items.SHIELD))

                .withHotbar(new ItemStack(Items.DIAMOND_SWORD), 0)
                .withHotbar(new ItemStack(Items.DIAMOND_AXE), 1)
                .withHotbar(new ItemStack(Items.BOW), 2)
                .withHotbar(new ItemStack(Items.GOLDEN_APPLE, 3), 3)
                .withHotbar(new ItemStack(Items.COOKED_BEEF, 64), 4)

                .withInventory(new ItemStack(Items.ARROW, 64), 0);
    }
}
