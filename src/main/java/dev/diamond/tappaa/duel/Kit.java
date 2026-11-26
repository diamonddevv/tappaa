package dev.diamond.tappaa.duel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Kit {


    public static final int HELMET_SLOT = 5;
    public static final int CHESTPLATE_SLOT = 6;
    public static final int LEGGINGS_SLOT = 7;
    public static final int BOOTS_SLOT = 8;
    public static final int OFFHAND_SLOT = 45;

    private static final int INVENTORY_SLOTS_OFFSET = 9;
    private static final int HOTBAR_SLOTS_OFFSET = 36;
    private static final int HOTBAR_SLOTS = 9;
    private static final int INVENTORY_SLOTS = 27;

    public static final Codec<Kit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SavedKits.IndexedItemStack.CODEC.listOf().fieldOf("stacks").forGetter(Kit::getIndexedStackList),
            Codec.INT.fieldOf("lives").forGetter(Kit::getLives),
            Codec.INT.fieldOf("respawn_ticks").forGetter(Kit::getRespawnTicks),
            Codec.BOOL.fieldOf("uses_stacks").forGetter(Kit::getUsesStacks)
    ).apply(instance, Kit::decodec));

    public static final int DEFAULT_KIT_LIVES = 3;
    public static final int DEFAULT_KIT_RESPAWN_TICKS = 5 * 20;
    private static final Identifier DEFAULT_KIT_ID = Tappaa.id("default");
    private static final String IS_OF_KIT_CUSTOM_DATA_TAG = Tappaa.id("is_of_kit").toString();

    final NonNullList<ItemStack> items;
    private int lives;
    private int respawnTicks;
    private boolean usesStacks;

    public Kit() {
        this.items = NonNullList.withSize(Inventory.INVENTORY_SIZE, ItemStack.EMPTY);
        this.lives = DEFAULT_KIT_LIVES;
        this.respawnTicks = DEFAULT_KIT_RESPAWN_TICKS;
    }
    private static Kit decodec(List<SavedKits.IndexedItemStack> stacks, int lives, int respawnTicks, boolean usesStacks) {
        Kit kit = Kit.fromMap(SavedKits.IndexedItemStack.decomposeList(stacks));
        kit.lives = lives;
        kit.respawnTicks = respawnTicks;
        kit.usesStacks = usesStacks;
        return kit;
    }

    public static List<ItemStack> getCopyOfItems(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();

        for (ItemStack item : player.getInventory()) {
            items.add(item.copy());
        }

        return items;
    }

    public static void saveDefaultKit(MinecraftServer minecraftServer) {
        if (!SavedKits.getSavedKits(minecraftServer).idExists(DEFAULT_KIT_ID)) {
            SavedKits.getSavedKits(minecraftServer).saveKit(
                    DEFAULT_KIT_ID, defaultKit(), true
            );
        }
    }

    public static NonNullList<ItemStack> copyPlayerInventory(ServerPlayer player) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(Inventory.INVENTORY_SIZE, ItemStack.EMPTY);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            stacks.set(i, player.getInventory().getItem(i).copy());
        }

        return stacks;
    }

    public static void restorePlayerInventory(ServerPlayer player, NonNullList<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            player.getInventory().setItem(i, stacks.get(i));
        };
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


    public Kit withHelmet(ItemStack stack) {
        setItem(, stack);
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
            setItem(HOTBAR_SLOTS_OFFSET + hotbarSlot, stack);
        }
        return this;
    }

    public Kit withInventory(ItemStack stack, int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < INVENTORY_SLOTS) {
            setItem(INVENTORY_SLOTS_OFFSET + inventorySlot, stack);
        }
        return this;
    }


    public static Kit fromMap(HashMap<Integer, ItemStack> items) {
        Kit kit = new Kit();

        kit.items.clear();
        for (int slot : items.keySet()) {
            kit.setItem(slot, items.get(slot));
        }

        return kit;
    }
    
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
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

    public void copyToPlayer(ServerPlayer player) {

        for (int i = 0; i < items.size(); i++) {
            player.getInventory().setItem(i, this.getItemForGiving(i));
        }
    }

    public static boolean isSlotOkay(int slot) {
        return true;
    }

    public @NonNull ItemStack getItemForGiving(int slot) {
        ItemStack stack = this.items.get(slot).copy();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(IS_OF_KIT_CUSTOM_DATA_TAG, true));
        return stack;
    }

}
