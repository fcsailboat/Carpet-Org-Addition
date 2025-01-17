package org.carpetorgaddition.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.BlockPos;

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
                throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }
    }

    private int parseInteger(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() != ' ') {
            return (int) Math.round(reader.readDouble());
        }
        throw CoordinateArgument.MISSING_BLOCK_POSITION.create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource) {
            String string = builder.getRemaining();
            Collection<CommandSource.RelativePosition> collection;
            collection = ((CommandSource) context.getSource()).getBlockPositionSuggestions();
            return CommandSource.suggestPositions(string, collection, builder, CommandManager.getCommandValidator(this::parse));
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
