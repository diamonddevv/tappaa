package dev.diamond.tappaa.duel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.diamond.tappaa.Tappaa;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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

    // todo: try to figure out where the player moves items to in their inventory so that it
    //       stays when the items are regiven
    //       -
    //       the best way might be with some nbt fuckery, store which slot an item WAS in when they were given
    //       then when they die, check what slot it is in now, back reference the old slot to fetch the item and
    //       put it in the new one

    // todo: add field to teleport all players within a certain radius of player who started duel when the duel
    //       begins

    // todo: add gamemode field

    // todo: potentially readd ui to customise kit items, using a hacked-up version of creative mode and the players inventory

    public static final int HELMET_SLOT = 39;
    public static final int CHESTPLATE_SLOT = 38;
    public static final int LEGGINGS_SLOT = 37;
    public static final int BOOTS_SLOT = 36;
    public static final int OFFHAND_SLOT = 40;

    private static final int INVENTORY_SLOTS_OFFSET = 9;
    private static final int HOTBAR_SLOTS_OFFSET = 0;
    private static final int HOTBAR_SLOTS = 9;
    private static final int INVENTORY_SLOTS = 27;

    public static final Codec<Kit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SavedKits.IndexedItemStack.CODEC.listOf().fieldOf("stacks").forGetter(Kit::getIndexedStackList),
            Codec.INT.fieldOf("lives")           .forGetter(Kit::getLives),
            Codec.INT.fieldOf("respawn_ticks")   .forGetter(Kit::getRespawnTicks),
            Codec.BOOL.fieldOf("uses_stacks")    .forGetter(Kit::getUsesStacks),
            Codec.BOOL.fieldOf("deletable")      .forGetter(Kit::deletable)
    ).apply(instance, Kit::decodec));

    public static final int DEFAULT_KIT_LIVES = 3;
    public static final int DEFAULT_KIT_RESPAWN_TICKS = 5 * 20;

    private static final String IS_OF_KIT_CUSTOM_DATA_TAG = Tappaa.id("is_of_kit").toString();

    private static final Identifier DEFAULT_KIT_ID = Tappaa.id("default");
    private static final Identifier DEFAULT_NO_STACK_KIT_ID = Tappaa.id("no_kit"); // ironically, no kit is a kit

    public HashMap<Integer, ItemStack> items;
    private int lives;
    private int respawnTicks;
    private boolean usesStacks;
    private boolean deletable;

    public Kit() {
        this.items = new HashMap<>();

        this.lives = DEFAULT_KIT_LIVES;
        this.respawnTicks = DEFAULT_KIT_RESPAWN_TICKS;
        this.usesStacks = true;
        this.deletable = false;
    }

    private static Kit decodec(List<SavedKits.IndexedItemStack> stacks, int lives, int respawnTicks, boolean usesStacks, boolean overwriteable) {
        Kit kit = Kit.fromIndexedStacks(stacks);
        kit.lives = lives;
        kit.respawnTicks = respawnTicks;
        kit.usesStacks = usesStacks;
        kit.deletable = overwriteable;
        return kit;
    }

    public static List<ItemStack> getCopyOfItems(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();

        for (ItemStack item : player.getInventory()) {
            items.add(item.copy());
        }

        return items;
    }

    public static void saveDefaultKits(MinecraftServer minecraftServer) {
        SavedKits kits = SavedKits.getSavedKits(minecraftServer);

        kits.saveKit(DEFAULT_KIT_ID, defaultKit(), false);
        kits.saveKit(DEFAULT_NO_STACK_KIT_ID, defaultNoStackKit(), false);
    }

    public static HashMap<Integer, ItemStack> copyPlayerInventory(ServerPlayer player) {
        HashMap<Integer, ItemStack> stacks = new HashMap<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            stacks.put(i, player.getInventory().getItem(i).copy());
        }

        return stacks;
    }

    public static void restorePlayerInventory(ServerPlayer player, HashMap<Integer, ItemStack> stacks, boolean modifiedNbt) {
        for (int slot : stacks.keySet()) {
            ItemStack stack = stacks.get(slot);
            player.getInventory().setItem(slot, modifiedNbt ? Kit.withModifiedDuelNbt(stack) : stack);
        }
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


    public Kit withLives(int lives) {
        this.lives = lives;
        return this;
    }

    public Kit withRespawnTicks(int respawnTicks) {
        this.respawnTicks = respawnTicks;
        return this;
    }

    public Kit withUsesStacks(boolean usesStacks) {
        this.usesStacks = usesStacks;
        return this;
    }

    public Kit withDeletable(boolean deletable) {
        this.deletable = deletable;
        return this;
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


    public void setStacks(HashMap<Integer, ItemStack> stacks) {
        this.items = stacks;
    }

    public void setItem(int slot, ItemStack stack) {
        this.items.put(slot, stack);
    }

    public ItemStack getItem(int slot) {
        return this.items.getOrDefault(slot, ItemStack.EMPTY);
    }

    public @NonNull ItemStack getItemForGiving(int slot) {
        ItemStack stack = this.getItem(slot).copy();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(IS_OF_KIT_CUSTOM_DATA_TAG, true));
        return stack;
    }

    private static ItemStack withModifiedDuelNbt(ItemStack stack) {
        ItemStack newStack = stack.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, newStack.copy(), nbt -> nbt.putBoolean(IS_OF_KIT_CUSTOM_DATA_TAG, true));
        return newStack;
    }

    private List<SavedKits.IndexedItemStack> getIndexedStackList() {
        List<SavedKits.IndexedItemStack> stacks = new ArrayList<>();
        for (int slot : items.keySet()) {
            stacks.add(new SavedKits.IndexedItemStack(slot, getItem(slot)));
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

    public boolean deletable() {
        return deletable;
    }

    public void copyToPlayer(ServerPlayer player) {

        for (int i : this.items.keySet()) {
            player.getInventory().setItem(i, this.getItemForGiving(i));
        }


    }

    public static Kit fromIndexedStacks(List<SavedKits.IndexedItemStack> items) {
        Kit kit = new Kit();

        kit.items.clear();
        for (SavedKits.IndexedItemStack stack : items) {
            kit.setItem(stack.idx(), stack.stack());
        }

        return kit;
    }

    public static boolean isSlotOkay(int slot) {
        return slot > 0 && slot <= OFFHAND_SLOT;
    }

    public static Kit defaultKit() {
        return new Kit()
                .withLives(3)
                .withRespawnTicks(5 * 20)
                .withUsesStacks(true)
                .withDeletable(false)

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

    public static Kit emptyKit() {
        return new Kit()
                .withLives(3)
                .withRespawnTicks(5 * 20)
                .withUsesStacks(true)
                .withDeletable(false);
    }

    public static Kit defaultNoStackKit() {
        return new Kit()
                .withLives(3)
                .withRespawnTicks(5 * 20)
                .withUsesStacks(false)
                .withDeletable(false);
    }

}
