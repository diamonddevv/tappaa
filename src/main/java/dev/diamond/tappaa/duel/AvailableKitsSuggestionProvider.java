package dev.diamond.tappaa.duel;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.diamond.tappaa.serial.SavedKits;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class AvailableKitsSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        SavedKits.getSavedKits(ctx.getSource().getServer()).getIdStrings().forEach(builder::suggest);
        return builder.buildFuture();
    }
}
