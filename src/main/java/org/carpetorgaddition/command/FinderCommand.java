package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.ServerPeriodicTaskManager;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.periodic.task.findtask.*;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.io.File;

public class FinderCommand {
    /**
     * 最大统计数量
     */
    public static final int MAXIMUM_STATISTICAL_COUNT = 30000;
    /**
     * 每个游戏刻最大查找时间
     */
    public static final long MAX_FIND_TIME = 200;
    /**
     * 任务执行的最大游戏刻数
     */
    public static final int MAX_TICK_COUNT = 50;
    /**
     * 村民的游戏内名称
     */
    public static final MutableText VILLAGER = TextUtils.translate("entity.minecraft.villager");
    /**
     * 查找超时时抛出异常的反馈消息
     */
    public static final String TIME_OUT = "carpet.commands.finder.timeout";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        dispatcher.register(CommandManager.literal("finder")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandFinder))
                .then(CommandManager.literal("block")
                        .then(CommandManager.argument("blockState", BlockStateArgumentType.blockState(commandRegistryAccess))
                                .executes(context -> blockFinder(context, 64))
                                .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> blockFinder(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(CommandManager.literal("from")
                                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.literal("to")
                                                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                                                .executes(FinderCommand::areaBlockFinder)))))))
                .then(CommandManager.literal("item")
                        .then(CommandManager.argument("itemStack", ItemPredicateArgumentType.itemPredicate(commandRegistryAccess))
                                .executes(context -> findItem(context, 64))
                                .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> findItem(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(CommandManager.literal("from")
                                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.literal("to")
                                                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                                                .executes(FinderCommand::areaItemFinder))))
                                        .then(CommandManager.literal("offline_player")
                                                .executes(FinderCommand::findItemFromOfflinePlayer)
                                                .then(CommandManager.literal("inventory")
                                                        .executes(FinderCommand::findItemFromOfflinePlayer))
                                                .then(CommandManager.literal("ender_chest")
                                                        .executes(FinderCommand::findItemFromOfflinePlayerEnderChest))))))
                .then(CommandManager.literal("trade")
                        .then(CommandManager.literal("item")
                                .then(CommandManager.argument("itemStack", ItemPredicateArgumentType.itemPredicate(commandRegistryAccess))
                                        .executes(context -> findTradeItem(context, 64))
                                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> findTradeItem(context, IntegerArgumentType.getInteger(context, "range"))))))
                        .then(CommandManager.literal("enchanted_book")
                                .then(CommandManager.argument("enchantment", RegistryEntryReferenceArgumentType.registryEntry(commandRegistryAccess, RegistryKeys.ENCHANTMENT))
                                        .executes(context -> findEnchantedBookTrade(context, 64))
                                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> findEnchantedBookTrade(context, IntegerArgumentType.getInteger(context, "range")))))))
                .then(CommandManager.literal("worldEater")
                        .requires(source -> CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION)
                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                        .executes(FinderCommand::mayAffectWorldEater)))));
    }

    private static SuggestionProvider<ServerCommandSource> suggestionDefaultDistance() {
        return (context, builder) -> CommandSource.suggestMatching(new String[]{"64", "128", "256"}, builder);
    }

    // 物品查找
    private static int findItem(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的位置，这是命令开始执行的坐标
        BlockPos sourceBlockPos = player.getBlockPos();
        // 查找周围容器中的物品
        World world = player.getWorld();
        ItemFindTask task = new ItemFindTask(world, predicate, new SelectionArea(world, sourceBlockPos, range), context);
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 区域查找物品
    private static int areaItemFinder(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取要查找的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 计算要查找的区域
        SelectionArea selectionArea = new SelectionArea(from, to);
        ItemFindTask task = new ItemFindTask(player.getWorld(), predicate, selectionArea, context);
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 从离线玩家身上查找物品
    private static int findItemFromOfflinePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        findItemFromOfflinePlayer(context, false);
        return 1;
    }

    private static int findItemFromOfflinePlayerEnderChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        findItemFromOfflinePlayer(context, true);
        return 0;
    }

    private static void findItemFromOfflinePlayer(CommandContext<ServerCommandSource> context, boolean enderChest) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        File[] files = player.server.getSavePath(WorldSavePath.PLAYERDATA).toFile().listFiles();
        if (files == null) {
            throw CommandUtils.createException("carpet.commands.finder.item.offline_player.unable_read_files");
        }
        UserCache userCache = player.server.getUserCache();
        if (userCache == null) {
            throw CommandUtils.createException("carpet.commands.finder.item.offline_player.unable_read_usercache");
        }
        ServerTask task;
        if (enderChest) {
            task = new OfflinePlayerEnderChestFindTask(context, userCache, player, files);
        } else {
            task = new OfflinePlayerFindTask(context, userCache, player, files);
        }
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
    }

    // 方块查找
    private static int blockFinder(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取要匹配的方块状态
        BlockStateArgument argument = BlockStateArgumentType.getBlockState(context, "blockState");
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.getBlockPos();
        ServerWorld world = player.getServerWorld();
        SelectionArea selectionArea = new SelectionArea(world, sourceBlockPos, range);
        ArgumentBlockPredicate predicate = new ArgumentBlockPredicate(argument);
        BlockFindTask task = new BlockFindTask(world, sourceBlockPos, selectionArea, context, predicate);
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 查找可能影响世吞运行的方块
    private static int mayAffectWorldEater(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.getBlockPos();
        ServerWorld world = player.getServerWorld();
        SelectionArea selectionArea = new SelectionArea(from, to);
        BlockBlockPredicate predicate = new BlockBlockPredicate();
        MayAffectWorldEaterBlockFindTask task = new MayAffectWorldEaterBlockFindTask(world, sourceBlockPos, selectionArea, context, predicate);
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
        return 0;
    }

    // 区域方块查找
    private static int areaBlockFinder(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取要匹配的方块状态
        BlockStateArgument argument = BlockStateArgumentType.getBlockState(context, "blockState");
        // 计算要查找的区域
        SelectionArea selectionArea = new SelectionArea(from, to);
        ArgumentBlockPredicate predicate = new ArgumentBlockPredicate(argument);
        // 添加查找任务
        BlockFindTask task = new BlockFindTask(player.getServerWorld(), player.getBlockPos(), selectionArea, context, predicate);
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 准备根据物品查找交易项
    private static int findTradeItem(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家对象
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取要匹配的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的坐标
        BlockPos sourcePos = player.getBlockPos();
        World world = player.getWorld();
        // 查找范围
        SelectionArea area = new SelectionArea(world, sourcePos, range);
        TradeItemFindTask task = new TradeItemFindTask(world, area, sourcePos, predicate, context);
        // 向任务管理器添加任务
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 准备查找出售指定附魔书的村民
    private static int findEnchantedBookTrade(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取需要查找的附魔
        Enchantment enchantment = RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment").value();
        // 获取玩家所在的位置
        BlockPos sourcePos = player.getBlockPos();
        World world = player.getWorld();
        // 查找范围
        SelectionArea area = new SelectionArea(world, sourcePos, range);
        TradeEnchantedBookFindTask task = new TradeEnchantedBookFindTask(world, area, sourcePos, context, enchantment);
        // 向任务管理器添加任务
        ServerPeriodicTaskManager.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 将物品数量转换为“多少组多少个”的形式
    public static MutableText showCount(ItemStack itemStack, int count, boolean inTheShulkerBox) {
        MutableText text = TextConstants.itemCount(count, itemStack.getMaxCount());
        // 如果包含在潜影盒内找到的物品，在数量上添加斜体效果
        return inTheShulkerBox ? TextUtils.toItalic(text) : text;
    }

    public interface BlockPredicate {
        boolean test(ServerWorld world, BlockPos pos);

        MutableText getName();
    }

    private record ArgumentBlockPredicate(BlockStateArgument argument) implements BlockPredicate {
        @Override
        public boolean test(ServerWorld world, BlockPos pos) {
            return argument.test(world, pos);
        }

        @Override
        public MutableText getName() {
            return argument.getBlockState().getBlock().getName();
        }
    }

    private record BlockBlockPredicate() implements BlockPredicate {
        @Override
        public boolean test(ServerWorld world, BlockPos pos) {
            BlockState blockState = world.getBlockState(pos);
            // 排除基岩，空气和流体
            if (blockState.isOf(Blocks.BEDROCK) || blockState.isAir() || blockState.getBlock() instanceof FluidBlock) {
                return false;
            }
            // 被活塞推动时会被破坏
            if (blockState.getPistonBehavior() == PistonBehavior.DESTROY) {
                return false;
            }
            // 高爆炸抗性
            if (blockState.getBlock().getBlastResistance() > 17) {
                return true;
            }
            // 不能推动（实体方块不能被推动）且含水
            boolean blockPiston = blockState.getBlock() instanceof BlockWithEntity || blockState.getPistonBehavior() == PistonBehavior.BLOCK;
            boolean hasWater = !blockState.getFluidState().isEmpty();
            if (blockPiston && hasWater) {
                return true;
            }
            // 含水，可以被推动，但下方8格全都有方块
            return hasWater && canPush(world, pos);
        }

        private boolean canPush(ServerWorld world, BlockPos pos) {
            for (int i = 1; i <= 8; i++) {
                BlockState blockState = world.getBlockState(pos.down(i));
                // 不可被推动的方块
                PistonBehavior pistonBehavior = blockState.getPistonBehavior();
                if (pistonBehavior == PistonBehavior.BLOCK) {
                    return true;
                }
                // 下方方块可以被推动
                if (pistonBehavior == PistonBehavior.DESTROY) {
                    return false;
                }
            }
            // 下方8格内都有方块
            return true;
        }

        @Override
        public MutableText getName() {
            return TextUtils.translate("carpet.commands.finder.may_affect_world_eater_block.name");
        }
    }
}
