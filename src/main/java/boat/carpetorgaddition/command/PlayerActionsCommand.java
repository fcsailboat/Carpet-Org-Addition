package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction;
import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction.Triple;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.misc.LibrarianTradeGroupManager;
import boat.carpetorgaddition.wheel.misc.LibrarianTradeTriples;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerActionsCommand extends AbstractServerCommand {
    private static final LocalizationKey LIBRARIAN_GROUP = PlayerActionCommand.KEY.then("librarian").then("group");

    public PlayerActionsCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(source -> CarpetOrgAdditionSettings.COMMAND_PLAYER_ACTION.value().hasPermission(source))
                .then(Commands.literal("librarian")
                        .then(Commands.literal("add")
                                .then(Commands.argument("group", StringArgumentType.string())
                                        .suggests(PlayerActionsCommand::suggestionsGroupName)
                                        .then(Commands.argument("enchantment", ResourceArgument.resource(this.access, Registries.ENCHANTMENT))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .suggests(PlayerActionCommand::suggestEnchantmentLevel)
                                                        .then(Commands.argument("price", IntegerArgumentType.integer(1, 64))
                                                                .suggests(PlayerActionCommand.suggestMixPrice(false))
                                                                .executes(context -> this.addLibrarianGroup(context, false))))
                                                .then(Commands.literal("max")
                                                        .then(Commands.argument("price", IntegerArgumentType.integer(1, 64))
                                                                .suggests(PlayerActionCommand.suggestMixPrice(true))
                                                                .executes(context -> this.addLibrarianGroup(context, true)))))))
                        .then(Commands.literal("addAll")
                                .then(Commands.argument("group", StringArgumentType.string())
                                        .suggests(PlayerActionsCommand::suggestionsGroupName)
                                        .executes(context -> addLibrarianGroupWithAllEnchantment(context, false))
                                        .then(Commands.literal("exclude")
                                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                                .executes(context -> addLibrarianGroupWithAllEnchantment(context, true)))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("group", StringArgumentType.string())
                                        .suggests(PlayerActionsCommand::suggestionsGroupName)
                                        .executes(this::removeLibrarianGroup)
                                        .then(Commands.argument("enchantment", ResourceArgument.resource(this.access, Registries.ENCHANTMENT))
                                                .suggests((context, builder) -> {
                                                    CommandSourceStack source = context.getSource();
                                                    MinecraftServer server = source.getServer();
                                                    LibrarianTradeGroupManager groupManager = ServerComponentCoordinator.of(server).getLibrarianTradeGroupManager();
                                                    String group = StringArgumentType.getString(context, "group");
                                                    List<Holder.Reference<Enchantment>> enchantments = groupManager.listEnchantment(group);
                                                    return SharedSuggestionProvider.suggestResource(enchantments.stream().map(ServerUtils::getId), builder);
                                                })
                                                .executes(this::removeLibrarianEnchantment)))))
        );
    }

    public static CompletableFuture<Suggestions> suggestionsGroupName(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        LibrarianTradeGroupManager groupManager = ServerComponentCoordinator.of(server).getLibrarianTradeGroupManager();
        return SharedSuggestionProvider.suggest(groupManager.listGroup().stream().map(StringArgumentType::escapeIfRequired), builder);
    }

    private int addLibrarianGroup(CommandContext<CommandSourceStack> context, boolean maxLevel) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        LibrarianTradeGroupManager groupManager = ServerComponentCoordinator.of(server).getLibrarianTradeGroupManager();
        String group = StringArgumentType.getString(context, "group");
        Holder.Reference<Enchantment> reference = ResourceArgument.getEnchantment(context, "enchantment");
        int level = maxLevel ? reference.value().getMaxLevel() : IntegerArgumentType.getInteger(context, "level");
        int price = IntegerArgumentType.getInteger(context, "price");
        LocalizationKey key = LIBRARIAN_GROUP.then("add");
        Component name = EnchantmentUtils.getName(reference);
        boolean added = groupManager.add(group, new Triple(reference, level, price));
        if (added) {
            Component message = key.then("success").translate(name, group);
            MessageUtils.sendMessage(source, message);
            return 1;
        }
        throw CommandUtils.createException(key.then("fail").translate(name, group));
    }

    private int addLibrarianGroupWithAllEnchantment(CommandContext<CommandSourceStack> context, boolean exclude) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        LibrarianTradeGroupManager groupManager = ServerComponentCoordinator.of(server).getLibrarianTradeGroupManager();
        String group = StringArgumentType.getString(context, "group");
        // 这里triple可能还没有创建，不能缓存对象
        int size = groupManager.getTriples(group).size();
        this.access.lookupOrThrow(Registries.ENCHANTMENT)
                .listElements()
                .filter(enchantment -> enchantment.is(EnchantmentTags.TRADEABLE))
                .forEach(enchantment -> {
                    int level = enchantment.value().getMaxLevel();
                    int price = Math.min(LibrarianTradeFindAction.getPriceRange(enchantment, level).getIntKey(), 64);
                    Triple triple = new Triple(enchantment, level, price);
                    groupManager.add(group, triple);
                });
        LibrarianTradeTriples triples = groupManager.getTriples(group);
        if (exclude) {
            BlockPos from = BlockPosArgument.getBlockPos(context, "from");
            BlockPos to = BlockPosArgument.getBlockPos(context, "to");
            ServerPlayer player = CommandUtils.getSourcePlayer(source);
            ServerLevel world = ServerUtils.getWorld(player);
            ServerUtils.getEntities(world, from, to, Villager.class).stream()
                    .flatMap(villager -> villager.getOffers().stream())
                    .forEach(triples::testAndRemove);
        }
        return triples.size() - size;
    }

    private int removeLibrarianGroup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        LibrarianTradeGroupManager groupManager = ServerComponentCoordinator.of(server).getLibrarianTradeGroupManager();
        String group = StringArgumentType.getString(context, "group");
        LocalizationKey key = LIBRARIAN_GROUP.then("remove").then("group");
        boolean removed = groupManager.remove(group);
        if (removed) {
            Component message = key.then("success").translate(group);
            MessageUtils.sendMessage(source, message);
            return 1;
        }
        throw CommandUtils.createException(key.then("fail").translate(group));
    }

    private int removeLibrarianEnchantment(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        LibrarianTradeGroupManager groupManager = ServerComponentCoordinator.of(server).getLibrarianTradeGroupManager();
        String group = StringArgumentType.getString(context, "group");
        Holder.Reference<Enchantment> reference = ResourceArgument.getEnchantment(context, "enchantment");
        LocalizationKey key = LIBRARIAN_GROUP.then("remove").then("enchantment");
        Component name = EnchantmentUtils.getName(reference);
        boolean removed = groupManager.remove(group, reference);
        if (removed) {
            Component message = key.then("success").translate(group, name);
            MessageUtils.sendMessage(source, message);
            return 1;
        }
        throw CommandUtils.createException(key.then("fail").translate(group, name));
    }

    @Override
    public String getDefaultName() {
        return "playerActions";
    }
}
