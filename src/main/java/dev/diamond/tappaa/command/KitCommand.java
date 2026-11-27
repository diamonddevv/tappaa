package dev.diamond.tappaa.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.diamond.tappaa.TappaaUtil;
import dev.diamond.tappaa.duel.AvailableKitsSuggestionProvider;
import dev.diamond.tappaa.duel.Kit;
import dev.diamond.tappaa.group.Group;
import dev.diamond.tappaa.group.GroupManager;
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
                        literal("from").then(
                                argument("player", EntityArgument.player()).executes(KitCommand::kitFromPlayer)
                        )
                ).then(
                        literal("additem").then(
                                argument("slot", SlotArgument.slot()).then(
                                        argument("item", ItemArgument.item(ctx)).executes(KitCommand::kitSetItem).then(
                                                argument("count", IntegerArgumentType.integer(0)).executes(KitCommand::kitSetItem)
                                        )
                                )
                        )
                ).then(
                        literal("save").then(
                                argument("id", IdentifierArgument.id()).executes(KitCommand::kitSave)
                        )
                ).then(
                        literal("load").then(
                                argument("id", IdentifierArgument.id())
                                        .suggests(new AvailableKitsSuggestionProvider())
                                        .executes(KitCommand::kitLoad)
                        )
                ).then(
                        literal("delete").then(
                                argument("id", IdentifierArgument.id())
                                        .suggests(new AvailableKitsSuggestionProvider())
                                        .executes(KitCommand::kitDelete)
                        )
                ).then(
                        literal("copyheld").then(
                                argument("slot", SlotArgument.slot()).then(
                                        argument("from", EntityArgument.player()).executes(KitCommand::kitSetItemHeld)
                                )
                        )
                ).then(
                        literal("option").then(
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
        );
    }



    private static int kitFromPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        group.get().getKit().setStacks(Kit.copyPlayerInventory(target));

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Kit updated to ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.playerTextComponent(target,ChatFormatting.GOLD))
                .append(Component.literal("'s inventory.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSetItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
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

        group.get().getKit().setItem(slot, stack);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Added ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.itemStackComponent(stack)) // item
                .append(Component.literal(" to kit.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSetItemHeld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        int slot = SlotArgument.getSlot(ctx, "slot");
        ServerPlayer target = EntityArgument.getPlayer(ctx, "from");

        ItemStack stack = target.getMainHandItem();

        if (!Kit.isSlotOkay(slot)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_BAD_SLOT.create();
        }

        group.get().getKit().setItem(slot, stack);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Added ").withStyle(ChatFormatting.GREEN))
                .append(TappaaUtil.itemStackComponent(stack)) // item
                .append(Component.literal(" to kit.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitSave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        Identifier id = IdentifierArgument.getId(ctx, "id");
        SavedKits kits = SavedKits.getSavedKits(ctx.getSource().getServer());

        if (kits.getKitFromId(id).map(Kit::overwriteable).orElse(false)) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_OVERWRITE_KIT.create();
        }

        kits.saveKit(id, group.get().getKit(), true);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Saved kit as [").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int kitLoad(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        Identifier id = IdentifierArgument.getId(ctx, "id");

        SavedKits.getSavedKits(ctx.getSource().getServer()).getKitFromId(id).ifPresentOrElse(kit -> {
            group.get().setKit(kit);

            PlayerFeedback.playYaySound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("Loaded kit [").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
            );
        }, () -> {
            PlayerFeedback.playBadSound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("No kit [").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("] exists!").withStyle(ChatFormatting.DARK_RED))
            );
        });

        return 1;
    }

    private static int kitDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        Identifier id = IdentifierArgument.getId(ctx, "id");

        boolean success = SavedKits.getSavedKits(ctx.getSource().getServer()).remove(id);

        if (success) {
            PlayerFeedback.playYaySound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("Deleted kit [").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("].").withStyle(ChatFormatting.GREEN))
            );
        } else {
            PlayerFeedback.playBadSound(src);
            src.sendSystemMessage(Component.empty()
                    .append(Component.literal("No such kit [").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("] exists to delete!").withStyle(ChatFormatting.DARK_RED))
            );
        }


        return 1;
    }


    private static int modifyLives(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        int i = IntegerArgumentType.getInteger(ctx, "lives");
        group.get().getKit().withLives(i);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Set lives to ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.valueOf(i)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static int modifyRespawnTicks(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        int i = IntegerArgumentType.getInteger(ctx, "respawnTicks");
        group.get().getKit().withRespawnTicks(i);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal("Set respawn ticks to ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.valueOf(i)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(". (").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.format("%.2f", i / 20f)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" seconds)").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }


    private static int modifyOwnItemsFlag(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer src = ctx.getSource().getPlayerOrException();

        Optional<Group> group = GroupManager.INSTANCE.getGroup(src);
        if (group.isEmpty()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_IN_GROUP.create();
        }

        if (group.get().getOwner() != src) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_NOT_GROUP_OWNER.create();
        }

        if (group.get().hasValidActiveDuel()) {
            PlayerFeedback.playBadSound(src);
            throw PlayerFeedback.ERROR_CANT_EDIT_KIT_IN_DUEL.create();
        }

        boolean bl = BoolArgumentType.getBool(ctx, "useOwnItems");
        group.get().getKit().withUsesStacks(bl);

        PlayerFeedback.playYaySound(src);
        src.sendSystemMessage(Component.empty()
                .append(Component.literal(bl ? "Set kit to use each player's own items." : "Set kit to use kit items.").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }



}
