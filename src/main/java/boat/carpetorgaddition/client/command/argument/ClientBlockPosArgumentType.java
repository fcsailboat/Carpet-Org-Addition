package boat.carpetorgaddition.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClientBlockPosArgumentType implements ArgumentType<BlockPos> {
    private static final Collection<String> EXAMPLES = List.of("0 0 0");

    private ClientBlockPosArgumentType() {
    }

    public static ClientBlockPosArgumentType blockPos() {
        return new ClientBlockPosArgumentType();
    }

    public static BlockPos getBlockPos(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, BlockPos.class);
    }

    @Override
    public BlockPos parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        int x = this.parseInteger(reader);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            int y = this.parseInteger(reader);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                int z = this.parseInteger(reader);
                return new BlockPos(x, y, z);
            } else {
                reader.setCursor(i);
                throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
        }
    }

    private int parseInteger(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() != ' ') {
            return (int) Math.round(reader.readDouble());
        }
        throw WorldCoordinate.ERROR_EXPECTED_INT.create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof SharedSuggestionProvider) {
            String string = builder.getRemaining();
            Collection<SharedSuggestionProvider.TextCoordinates> collection;
            collection = ((SharedSuggestionProvider) context.getSource()).getRelevantCoordinates();
            return SharedSuggestionProvider.suggestCoordinates(string, collection, builder, Commands.createValidator(this::parse));
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
