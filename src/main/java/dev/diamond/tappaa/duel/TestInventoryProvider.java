package dev.diamond.tappaa.duel;

import dev.diamond.tappaa.mixin.EntityEquipmentAccessor;
import dev.diamond.tappaa.mixin.InventoryAccessor;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;

public class TestInventoryProvider {
    /*
     * THIS IS A TEST
     */

    private final ServerPlayer player;
    private NonNullList<ItemStack> items;

    private NonNullList<ItemStack> preItems;
    private final EnumMap<EquipmentSlot, ItemStack> preEquipment;

    public TestInventoryProvider(ServerPlayer player, NonNullList<ItemStack> items) {
        this.player = player;
        this.items = items;

        this.preItems = player.getInventory().getNonEquipmentItems();
        this.preEquipment = ((EntityEquipmentAccessor)((InventoryAccessor)player.getInventory()).getEquipment()).getItems();

        player.openMenu(new MenuProvider() {
            @Override
            public @NonNull Component getDisplayName() {
                return Component.empty();
            }

            @Override
            public @NonNull AbstractContainerMenu createMenu(int containerId, @NonNull Inventory inventory, @NonNull Player player) {
                return new InventoryMenu(inventory, true, player);
            }
        });

    }

    public void closed() {
        for (int i = 0; i < preItems.size(); i++) {
            this.player.getInventory().setItem(i, preItems.get(i));
        }

        for (EquipmentSlot slot : preEquipment.keySet()) {
            ((InventoryAccessor)player.getInventory()).getEquipment().set(slot, preEquipment.get(slot));
        }
    }

}
