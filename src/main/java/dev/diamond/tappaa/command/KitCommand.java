package dev.diamond.tappaa.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.duel.AvailableKitsSuggestionProvider;
import dev.diamond.tappaa.duel.Kit;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class KitCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {
        dispatcher.register(
                literal("kit").then(
                        literal("frominventory").then(
                                KitCommand.kitArgument().then(
                                        argument("player", EntityArgument.player()).executes(KitCommand::kitFromPlayer)
                                )
                        )
                ).then(
                        literal("fromitem").then(
                                KitCommand.kitArgument().then(
                                        argument("slot", SlotArgument.slot()).executes(KitCommand::kitClearSlot).then(
                                                argument("item", ItemArgument.item(ctx)).executes(KitCommand::kitSetItem).then(
                                                        argument("count", IntegerArgumentType.integer(0)).executes(KitCommand::kitSetItem)
                                                )
                                        )
                                )
                        )
                ).then(
                        literal("create").then(
                                argument("id", IdentifierArgument.id()).executes(KitCommand::kitCreate)
                        )
                ).then(
                        literal("delete").then(
                                KitCommand.kitArgument()
                                        .executes(KitCommand::kitDelete)
                        )
                ).then(
                        literal("fromheld").then(
                                KitCommand.kitArgument().then(
                                        argument("slot", SlotArgument.slot()).then(
                                                argument("from", EntityArgument.player()).executes(KitCommand::kitSetItemHeld)
                                        )
                                )
                        )
                ).then(
                        literal("option").then(
                                KitCommand.kitArgument().then(
                                        literal("lives").then(
                                                argument("lives", IntegerArgumentType.integer(1)).executes(KitCommand::modifyLives)
                                        )
                                ).then(
                                        literal("respawn").then(
                                                argument("respawnTicks", IntegerArgumentType.integer(0)).executes(KitCommand::modifyRespawnTicks)
                                        )
                                ).then(
                                        literal("ownitems").then(
                                                argument("useOwnItems", BoolArgumentType.bool()).executes(KitCommand::modifyOwnItemsFlag)
                                        )
                                )
                        )
                )
        );
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Identifier> kitArgument() {
        return argument("kit", IdentifierArgument.id())
                .suggests(new AvailableKitsSuggestionProvider());
    }

    private static int kitFromPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        kit.get().setStacks(Kit.copyPlayerInventory(target));
        kits.saveKit(kitId, kit.get(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Items in kit [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("] updated to ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(target,ChatFormatting.GOLD))
                .append(Component.literal("'s inventory.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSetItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT.create();
        }

        int slot = SlotArgument.getSlot(ctx, "slot");
        ItemInput item = ItemArgument.getItem(ctx, "item");

        int count;
        try {
            count = IntegerArgumentType.getInteger(ctx, "count");
        } catch (IllegalArgumentException _) {
            count = 1;
        }

        ItemStack stack = item.createItemStack(count);


        if (!Kit.isSlotOkay(slot)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_BAD_SLOT.create();
        }

        kit.get().setItem(slot, stack);
        kits.saveKit(kitId, kit.get(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Added ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.itemStackComponent(stack)) // item
                .append(Component.literal(" to kit [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitClearSlot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT.create();
        }

        int slot = SlotArgument.getSlot(ctx, "slot");


        if (!Kit.isSlotOkay(slot)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_BAD_SLOT.create();
        }

        kit.get().clearItem(slot);
        kits.saveKit(kitId, kit.get(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Cleared slot in kit [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSetItemHeld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT.create();
        }

        int slot = SlotArgument.getSlot(ctx, "slot");
        ServerPlayer target = EntityArgument.getPlayer(ctx, "from");

        ItemStack stack = target.getMainHandItem();

        if (!Kit.isSlotOkay(slot)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_BAD_SLOT.create();
        }

        kit.get().setItem(slot, stack);
        kits.saveKit(kitId, kit.get(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Added ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.itemStackComponent(stack)) // item
                .append(Component.literal(" to kit [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier id = IdentifierArgument.getId(ctx, "id");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());

        if (kits.idExists(id)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_ALREADY_EXISTS.create();
        }

        Kit template = Kit.emptyKit();
        template.withMutable(true);
        kits.saveKit(id, template, false);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Created new empty kit as [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_DELETE_KIT.create();
        }

        boolean success = kits.remove(kitId);

        if (success) {
            PlayerFeedback.playYaySound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("Deleted kit [").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
            );
        } else {
            PlayerFeedback.playBadSound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("Failed to delete kit [").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("] (this is probably a bug :[ ).").withStyle(ChatFormatting.DARK_RED))
            );
        }


        return 1;
    }


    private static int modifyLives(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT.create();
        }

        int i = IntegerArgumentType.getInteger(ctx, "lives");
        kit.get().withLives(i);

        kits.saveKit(kitId, kit.get(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Set lives of kit [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("] to ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.valueOf(i)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int modifyRespawnTicks(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT.create();
        }

        int i = IntegerArgumentType.getInteger(ctx, "respawnTicks");
        kit.get().withRespawnTicks(i);
        kits.saveKit(kitId, kit.get(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Set respawn ticks of kit [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("] to ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.valueOf(i)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(". (").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.format("%.2f", i / 20f)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" seconds)").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }


    private static int modifyOwnItemsFlag(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Identifier kitId = IdentifierArgument.getId(ctx, "kit");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());
        Optional<Kit> kit = kits.getKitFromId(kitId);

        if (kit.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_KIT_DOESNT_EXIST.create();
        }

        if (!kit.get().mutable()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT.create();
        }

        boolean bl = BoolArgumentType.getBool(ctx, "useOwnItems");
        kit.get().withUsesStacks(bl);
        kits.saveKit(kitId, kit.get(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Set kit [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(kitId.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("] to use ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(bl ? "each player's own items." : "kit items.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }



}
