package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.periodic.task.search.*;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.predicate.BlockStatePredicate;
import boat.carpetorgaddition.wheel.predicate.EnchantedBookPredicate;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.traverser.BlockEntityTraverser;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import boat.carpetorgaddition.wheel.traverser.WorldTraverser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public class FinderCommand extends AbstractServerCommand {
    /**
     * 最大统计数量
     */
    public static final int MAXIMUM_STATISTICAL_COUNT = 30000;
    /**
     * 每个游戏刻最大查找时间
     */
    public static final long TIME_SLICE = 30L;
    /**
     * 最大查找时间
     */
    public static final long MAX_SEARCH_TIME = 20L * 1000L;
    /**
     * 最大查找半径
     */
    public static final int MAX_HORIZONTAL_RANGE = 512;
    /**
     * 村民的游戏内名称
     */
    public static final Component VILLAGER = LocalizationKey.literal("entity.minecraft.villager").translate();
    public static final String FINDER_BLOCK = "finder.block";
    public static final String FINDER_ITEM = "finder.item";
    public static final String FINDER_ITEM_FROM_OFFLINE_PLAYER = "finder.item.from.offline_player";
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("finder");

    public FinderCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandFinder))
                .then(Commands.literal("block")
                        .requires(PermissionManager.register(FINDER_BLOCK, PermissionLevel.PASS))
                        .then(Commands.argument("blockState", BlockPredicateArgument.blockPredicate(this.access))
                                .executes(context -> blockFinder(context, 64))
                                .then(Commands.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> blockFinder(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(Commands.literal("from")
                                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                                .then(Commands.literal("to")
                                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                                .executes(this::areaBlockSearch)))))))
                .then(Commands.literal("item")
                        .requires(PermissionManager.register(FINDER_ITEM, PermissionLevel.PASS))
                        .then(Commands.argument("itemStack", ItemPredicateArgument.itemPredicate(this.access))
                                .executes(context -> searchItem(context, 64))
                                .then(Commands.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> searchItem(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(Commands.literal("from")
                                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                                .then(Commands.literal("to")
                                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                                .executes(this::areaItemFinder))))
                                        .then(Commands.literal("offline_player")
                                                .requires(PermissionManager.register(FINDER_ITEM_FROM_OFFLINE_PLAYER, PermissionLevel.PASS))
                                                .executes(this::searchItemFromOfflinePlayer)))))
                .then(Commands.literal("trade")
                        .requires(PermissionManager.register("finder.trade", PermissionLevel.PASS))
                        .then(Commands.literal("item")
                                .then(Commands.argument("itemStack", ItemPredicateArgument.itemPredicate(this.access))
                                        .executes(context -> searchTradeItem(context, 64))
                                        .then(Commands.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> searchTradeItem(context, IntegerArgumentType.getInteger(context, "range"))))))
                        .then(Commands.literal("enchanted_book")
                                .then(Commands.argument("enchantment", ResourceArgument.resource(this.access, Registries.ENCHANTMENT))
                                        .executes(context -> searchEnchantedBookTrade(context, 64))
                                        .then(Commands.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> searchEnchantedBookTrade(context, IntegerArgumentType.getInteger(context, "range")))))))
                .then(Commands.literal("worldEater")
                        .requires(((Predicate<CommandSourceStack>) _ -> CarpetOrgAdditionConstants.isEnableHiddenFunction())
                                .and(PermissionManager.registerHiddenCommand("finder.worldEater", PermissionLevel.PASS)))
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(this::mayAffectWorldEater)))));
    }

    private SuggestionProvider<CommandSourceStack> suggestionDefaultDistance() {
        return (_, builder) -> SharedSuggestionProvider.suggest(new String[]{"64", "128", "256", "512"}, builder);
    }

    /**
     * 物品查找
     */
    private int searchItem(CommandContext<CommandSourceStack> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的位置，这是命令开始执行的坐标
        BlockPos sourceBlockPos = player.blockPosition();
        // 查找周围容器中的物品
        Level world = ServerUtils.getWorld(player);
        BlockEntityTraverser traverser = new BlockEntityTraverser(world, sourceBlockPos, range);
        this.checkBoxSize(traverser);
        CommandSourceStack source = context.getSource();
        ItemSearchTask task = new ItemSearchTask(world, predicate, traverser, source);
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 区域查找物品
     */
    private int areaItemFinder(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgument.getBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getBlockPos(context, "to");
        // 获取要查找的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 计算要查找的区域
        Level world = ServerUtils.getWorld(player);
        BlockEntityTraverser traverser = new BlockEntityTraverser(world, from, to);
        this.checkBoxSize(traverser);
        CommandSourceStack source = context.getSource();
        ItemSearchTask task = new ItemSearchTask(world, predicate, traverser, source);
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 从离线玩家身上查找物品
     */
    private int searchItemFromOfflinePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        CommandSourceStack source = context.getSource();
        ServerTask task = new OfflinePlayerSearchTask(source, predicate, player);
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 方块查找
     */
    private int blockFinder(CommandContext<CommandSourceStack> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.blockPosition();
        ServerLevel world = ServerUtils.getWorld(player);
        BlockPosTraverser traverser = new BlockPosTraverser(world, sourceBlockPos, range);
        this.checkBoxSize(traverser);
        BlockStatePredicate predicate = BlockStatePredicate.ofPredicate(context, "blockState");
        CommandSourceStack source = context.getSource();
        BlockSearchTask task = new BlockSearchTask(world, sourceBlockPos, traverser, source, predicate);
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 查找可能影响世吞运行的方块
     */
    private int mayAffectWorldEater(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgument.getBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getBlockPos(context, "to");
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.blockPosition();
        ServerLevel world = ServerUtils.getWorld(player);
        BlockPosTraverser traverser = new BlockPosTraverser(from, to);
        this.checkBoxSize(traverser);
        BlockStatePredicate predicate = BlockStatePredicate.ofWorldEater();
        CommandSourceStack source = context.getSource();
        BlockSearchTask task = new BlockSearchTask(world, sourceBlockPos, traverser, source, predicate);
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 区域方块查找
     */
    private int areaBlockSearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgument.getBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getBlockPos(context, "to");
        // 计算要查找的区域
        BlockPosTraverser traverser = new BlockPosTraverser(from, to);
        this.checkBoxSize(traverser);
        BlockStatePredicate predicate = BlockStatePredicate.ofPredicate(context, "blockState");
        // 添加查找任务
        CommandSourceStack source = context.getSource();
        BlockSearchTask task = new BlockSearchTask(ServerUtils.getWorld(player), player.blockPosition(), traverser, source, predicate);
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 准备根据物品查找交易项
     */
    private int searchTradeItem(CommandContext<CommandSourceStack> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家对象
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取要匹配的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的坐标
        BlockPos sourcePos = player.blockPosition();
        Level world = ServerUtils.getWorld(player);
        // 查找范围
        BlockPosTraverser traverser = new BlockPosTraverser(world, sourcePos, range);
        this.checkBoxSize(traverser);
        CommandSourceStack source = context.getSource();
        TradeItemSearchTask task = new TradeItemSearchTask(world, traverser, sourcePos, predicate, source);
        // 向任务管理器添加任务
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 准备查找出售指定附魔书的村民
     */
    private int searchEnchantedBookTrade(CommandContext<CommandSourceStack> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取需要查找的附魔
        Enchantment enchantment = ResourceArgument.getEnchantment(context, "enchantment").value();
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        EnchantedBookPredicate predicate = new EnchantedBookPredicate(server, enchantment);
        // 获取玩家所在的位置
        BlockPos sourcePos = player.blockPosition();
        Level world = ServerUtils.getWorld(player);
        // 查找范围
        BlockPosTraverser traverser = new BlockPosTraverser(world, sourcePos, range);
        this.checkBoxSize(traverser);
        TradeEnchantedBookSearchTask task = new TradeEnchantedBookSearchTask(world, traverser, sourcePos, source, predicate);
        // 向任务管理器添加任务
        ServerComponentCoordinator.getCoordinator(server).getServerTaskManager().addTask(task);
        return 1;
    }

    private void checkBoxSize(WorldTraverser<?> traverser) throws CommandSyntaxException {
        int max = (MAX_HORIZONTAL_RANGE << 1) + 1;
        if (traverser.length() > max || traverser.width() > max) {
            throw CommandUtils.createException(KEY.then("toobig").translate(max));
        }
    }

    /**
     * 将物品数量转换为“多少组多少个”的形式
     */
    public static Component showCount(ItemStack itemStack, int count, boolean inTheShulkerBox) {
        TextBuilder builder = TextBuilder.of(TextProvider.itemCount(count, itemStack.getMaxStackSize()));
        // 如果包含在潜影盒内找到的物品，在数量上添加斜体效果
        return inTheShulkerBox ? builder.setItalic().build() : builder.build();
    }

    @Override
    public String getDefaultName() {
        return "finder";
    }
}
