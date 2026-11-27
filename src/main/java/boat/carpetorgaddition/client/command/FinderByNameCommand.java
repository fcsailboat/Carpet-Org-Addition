package boat.carpetorgaddition.client.command;

import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType;
import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType.ClientBlockArgumentType;
import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType.ClientItemArgumentType;
import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.network.c2s.ObjectSearchTaskC2SPacket;
import boat.carpetorgaddition.network.codec.ObjectSearchTaskCodecs;
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
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class FinderByNameCommand extends AbstractClientCommand {
    public FinderByNameCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(ClientCommands.literal(name)
                .then(ClientCommands.literal("item")
                        .then(ClientCommands.argument("item", new ClientItemArgumentType(true))
                                .executes(context -> searchItem(context, FinderCommand.DEFAULT_HORIZONTAL_RADIUS))
                                .then(ClientCommands.argument("radius", IntegerArgumentType.integer(0, FinderCommand.MAX_HORIZONTAL_RADIUS))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> searchItem(context, IntegerArgumentType.getInteger(context, "radius"))))
                                .then(ClientCommands.literal("from")
                                        .then(ClientCommands.literal("offline_player")
                                                .executes(this::searchItem)))))
                .then(ClientCommands.literal("block")
                        .then(ClientCommands.argument("block", new ClientBlockArgumentType(true))
                                .executes(context -> searchBlock(context, FinderCommand.DEFAULT_HORIZONTAL_RADIUS))
                                .then(ClientCommands.argument("radius", IntegerArgumentType.integer(0, FinderCommand.MAX_HORIZONTAL_RADIUS))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> searchBlock(context, IntegerArgumentType.getInteger(context, "radius"))))))
                .then(ClientCommands.literal("trade")
                        .then(ClientCommands.literal("item")
                                .then(ClientCommands.argument("item", new ClientItemArgumentType(true))
                                        .executes(context -> searchTradeItem(context, FinderCommand.DEFAULT_HORIZONTAL_RADIUS))
                                        .then(ClientCommands.argument("radius", IntegerArgumentType.integer(0, FinderCommand.MAX_HORIZONTAL_RADIUS))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> searchTradeItem(context, IntegerArgumentType.getInteger(context, "radius"))))))
                        .then(ClientCommands.literal("enchantment")
                                .then(ClientCommands.argument("enchantment", new ClientObjectArgumentType.ClientEnchantmentArgumentType(true))
                                        .executes(context -> searchTradeEnchantedBook(context, FinderCommand.DEFAULT_HORIZONTAL_RADIUS))
                                        .then(ClientCommands.argument("radius", IntegerArgumentType.integer(0, FinderCommand.MAX_HORIZONTAL_RADIUS))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> searchTradeEnchantedBook(context, IntegerArgumentType.getInteger(context, "radius"))))))));
    }

    private SuggestionProvider<FabricClientCommandSource> suggestionDefaultDistance() {
        return (_, builder) -> SharedSuggestionProvider.suggest(FinderCommand.SUGGESTED_RADIUS, builder);
    }

    private int searchItem(CommandContext<FabricClientCommandSource> context, int radius) {
        List<Item> list = getItemList(context);
        String name = CommandUtils.getArgumentLiteral(context, "item").orElseThrow();
        ObjectSearchTaskCodecs.ItemSearchContext itemSearchContext = new ObjectSearchTaskCodecs.ItemSearchContext(radius, list);
        JsonObject json = ObjectSearchTaskCodecs.ITEM_SEARCH_CODEC.encode(itemSearchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(ObjectSearchTaskC2SPacket.Type.ITEM, name, json);
        ClientPlayNetworking.send(packet);
        return list.size();
    }

    private int searchItem(CommandContext<FabricClientCommandSource> context) {
        List<Item> list = getItemList(context);
        String name = CommandUtils.getArgumentLiteral(context, "item").orElseThrow();
        ObjectSearchTaskCodecs.OfflinePlayerItemSearchContext searchContext = new ObjectSearchTaskCodecs.OfflinePlayerItemSearchContext(list);
        JsonObject json = ObjectSearchTaskCodecs.OFFLINE_PLAYER_SEARCH_CODEC.encode(searchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(ObjectSearchTaskC2SPacket.Type.OFFLINE_PLAYER_ITEM, name, json);
        ClientPlayNetworking.send(packet);
        return list.size();
    }

    private int searchBlock(CommandContext<FabricClientCommandSource> context, int radius) {
        List<Block> list = ClientObjectArgumentType.getType(context, "block").stream()
                .filter(t -> t instanceof Block)
                .map(t -> (Block) t)
                .toList();
        String name = CommandUtils.getArgumentLiteral(context, "block").orElseThrow();
        ObjectSearchTaskCodecs.BlockSearchContext searchContext = new ObjectSearchTaskCodecs.BlockSearchContext(radius, list);
        JsonObject json = ObjectSearchTaskCodecs.BLOCK_SEARCH_CODEC.encode(searchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(ObjectSearchTaskC2SPacket.Type.BLOCK, name, json);
        ClientPlayNetworking.send(packet);
        return list.size();
    }

    private int searchTradeItem(CommandContext<FabricClientCommandSource> context, int radius) {
        List<Item> list = getItemList(context);
        String name = CommandUtils.getArgumentLiteral(context, "item").orElseThrow();
        ObjectSearchTaskCodecs.TradeItemSearchContext searchContext = new ObjectSearchTaskCodecs.TradeItemSearchContext(radius, list);
        JsonObject json = ObjectSearchTaskCodecs.TRADE_ITEM_SEARCH_CODEC.encode(searchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(ObjectSearchTaskC2SPacket.Type.TRADE_ITEM, name, json);
        ClientPlayNetworking.send(packet);
        return list.size();
    }

    @SuppressWarnings("unchecked")
    private int searchTradeEnchantedBook(CommandContext<FabricClientCommandSource> context, int range) {
        List<Holder<Enchantment>> enchantments = ClientObjectArgumentType.getType(context, "enchantment")
                .stream()
                .filter(t -> t instanceof Holder<?> holder && holder.value() instanceof Enchantment)
                .map(t -> (Holder<Enchantment>) t)
                .toList();
        String name = CommandUtils.getArgumentLiteral(context, "enchantment").orElseThrow();
        ObjectSearchTaskCodecs.TradeEnchantedBookSearchContext searchContext = new ObjectSearchTaskCodecs.TradeEnchantedBookSearchContext(range, enchantments);
        JsonObject json = ObjectSearchTaskCodecs.TRADE_ENCHANTED_BOOK_SEARCH_CODEC.encode(searchContext);
        ObjectSearchTaskC2SPacket packet = new ObjectSearchTaskC2SPacket(ObjectSearchTaskC2SPacket.Type.TRADE_ENCHANTED_BOOK, name, json);
        ClientPlayNetworking.send(packet);
        return enchantments.size();
    }

    private List<Item> getItemList(CommandContext<FabricClientCommandSource> context) {
        return ClientObjectArgumentType.getType(context, "item").stream()
                .filter(t -> t instanceof Item)
                .map(t -> (Item) t)
                .toList();
    }

    @Override
    public String getDefaultName() {
        return "finderbyname";
    }
}
