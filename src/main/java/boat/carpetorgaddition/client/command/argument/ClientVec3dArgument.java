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
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ClientVec3dArgument implements ArgumentType<Vec3> {
    private ClientVec3dArgument() {
    }

    public static ClientVec3dArgument blockPos() {
        return new ClientVec3dArgument();
    }

    public static Vec3 getVec3d(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, Vec3.class);
    }

    @Override
    public Vec3 parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        double x = this.readDouble(reader);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            double y = this.readDouble(reader);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                double z = this.readDouble(reader);
                return new Vec3(x, y, z);
            } else {
                reader.setCursor(i);
                throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
        }
    }

    private double readDouble(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() != ' ') {
            return reader.readDouble();
        }
        throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof SharedSuggestionProvider provider) {
            String string = builder.getRemaining();
            Collection<SharedSuggestionProvider.TextCoordinates> collection = provider.getAbsoluteCoordinates();
            return SharedSuggestionProvider.suggestCoordinates(string, collection, builder, Commands.createValidator(this::parse));
        } else {
            return Suggestions.empty();
        }
    }
}
