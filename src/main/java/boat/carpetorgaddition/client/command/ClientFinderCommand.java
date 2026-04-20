package boat.carpetorgaddition.client.command;

import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType;
import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType.ClientBlockArgumentType;
import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType.ClientItemArgumentType;
import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.network.c2s.ObjectSearchTaskC2SPacket;
import boat.carpetorgaddition.network.c2s.ObjectSearchTaskC2SPacket.Type;
import boat.carpetorgaddition.network.codec.ObjectSearchTaskCodecs;
import boat.carpetorgaddition.network.codec.ObjectSearchTaskCodecs.BlockSearchContext;
import boat.carpetorgaddition.network.codec.ObjectSearchTaskCodecs.ItemSearchContext;
import boat.carpetorgaddition.network.codec.ObjectSearchTaskCodecs.OfflinePlayerItemSearchContext;
import boat.carpetorgaddition.util.CommandUtils;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ClientFinderCommand extends AbstractClientCommand {
    public ClientFinderCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(ClientCommands.literal(name)
                .then(ClientCommands.literal("item")
                        .then(ClientCommands.argument("item", new ClientItemArgumentType(true))
                                .executes(context -> searchItem(context, 64))
                                .then(ClientCommands.argument("range", IntegerArgumentType.integer(0, FinderCommand.MAX_HORIZONTAL_RANGE))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> searchItem(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(ClientCommands.literal("from")
                                        .then(ClientCommands.literal("offline_player")
                                                .executes(this::searchItem)))))
                .then(ClientCommands.literal("block")
                        .then(ClientCommands.argument("block", new ClientBlockArgumentType(true))
                                .executes(context -> searchBlock(context, 64))
                                .then(ClientCommands.argument("range", IntegerArgumentType.integer(0, FinderCommand.MAX_HORIZONTAL_RANGE))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> searchBlock(context, IntegerArgumentType.getInteger(context, "range")))))));
    }

    private SuggestionProvider<FabricClientCommandSource> suggestionDefaultDistance() {
        return (_, builder) -> SharedSuggestionProvider.suggest(new String[]{"64", "128", "256", "512"}, builder);
    }

    private int searchItem(CommandContext<FabricClientCommandSource> context, int range) {
        List<Item> list = getItemList(context);
        String name = CommandUtils.getArgumentLiteral(context, "item").orElseThrow();
        ItemSearchContext itemSearchContext = new ItemSearchContext(range, list);
        JsonObject json = ObjectSearchTaskCodecs.ITEM_SEARCH_CODEC.encode(itemSearchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(Type.ITEM, name, json);
        ClientPlayNetworking.send(packet);
        return list.size();
    }

    private int searchItem(CommandContext<FabricClientCommandSource> context) {
        List<Item> list = getItemList(context);
        String name = CommandUtils.getArgumentLiteral(context, "item").orElseThrow();
        OfflinePlayerItemSearchContext searchContext = new OfflinePlayerItemSearchContext(list);
        JsonObject json = ObjectSearchTaskCodecs.OFFLINE_PLAYER_SEARCH_CODEC.encode(searchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(Type.OFFLINE_PLAYER_ITEM, name, json);
        ClientPlayNetworking.send(packet);
        return list.size();
    }

    private int searchBlock(CommandContext<FabricClientCommandSource> context, int range) {
        List<Block> list = ClientObjectArgumentType.getType(context, "block").stream()
                .filter(t -> t instanceof Block)
                .map(t -> (Block) t)
                .toList();
        String name = CommandUtils.getArgumentLiteral(context, "block").orElseThrow();
        BlockSearchContext searchContext = new BlockSearchContext(range, list);
        JsonObject json = ObjectSearchTaskCodecs.BLOCK_SEARCH_CODEC.encode(searchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(Type.BLOCK, name, json);
        ClientPlayNetworking.send(packet);
        return list.size();
    }

    private List<Item> getItemList(CommandContext<FabricClientCommandSource> context) {
        return ClientObjectArgumentType.getType(context, "item").stream()
                .filter(t -> t instanceof Item)
                .map(t -> (Item) t)
                .toList();
    }

    @Override

    public String getDefaultName() {
        return "clientfinder";
    }
}
