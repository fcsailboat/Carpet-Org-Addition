package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ItemPredicateArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;

import java.util.function.Predicate;

/**
 * 测试用，不会添加到游戏
 */
@SuppressWarnings({"unused", "CommentedOutCode", "GrazieInspection"})
public class CarpetOrgAdditionTestCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext) {
        dispatcher.register(CommandManager.literal("carpetOrgAdditionTest")
                .requires(source -> FabricLoader.getInstance().isDevelopmentEnvironment())
/*                .then(CommandManager.literal("listEnchantBookFactory")
                        .executes(context -> listEnchantBookFactory(context.getSource())))*/
                .then(CommandManager.literal("getIndex")
                        .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                .executes(CarpetOrgAdditionTestCommand::getItemIndex)))
                .then(CommandManager.literal("getBlockHardness")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(CarpetOrgAdditionTestCommand::getBlockHardness)))
                .then(CommandManager.literal("randomTick")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                        .executes(CarpetOrgAdditionTestCommand::randomTick))))
                .then(CommandManager.literal("switchCraftUpdate")
                        .executes(context -> switchCraftUpdate())));
    }

    //列出图书管理员所有可交易的附魔书
/*    private static int listEnchantBookFactory(ServerCommandSource source, CommandRegistryAccess commandRegistryAccess) {
        List<Enchantment> list = RegistryEntryReferenceArgumentType.registryEntry(commandRegistryAccess, RegistryKeys.ENCHANTMENT).getExamples()
                .stream().filter(Enchantment::isAvailableForEnchantedBookOffer).toList();
        ServerPlayerEntity player = source.getPlayer();
        if (player != null) {
            player.sendMessage(Text.of("图书管理员所有可交易的附魔书:"));
            for (Enchantment enchantment : list) {
                player.sendMessage(enchantment.getName(enchantment.getMaxLevel()));
            }
        }
        return 1;
    }*/

    private static int getItemIndex(CommandContext<ServerCommandSource> context) {
        Predicate<ItemStack> item = ItemPredicateArgumentType.getItemStackPredicate(context, "item");
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return -1;
        }
        DefaultedList<ItemStack> main = player.getInventory().main;
        for (int index = 0; index < main.size(); index++) {
            if (item.test(main.get(index))) {
                MessageUtils.sendMessage(player, Text.literal(main.get(index).getName().getString() + "在索引" + index));
                return index;
            }
        }
        return -1;
    }

    /*
     * 测试结果：
     * 代码调整后：
     * 黑曜石：20296，2064，2041，2057，2046，2040
     * 切石机：20254，20273，20145，20178，19990
     * 基岩：3941，3898，3801，3799，3810
     * 石头：20876，20995，20996，20890，20978
     * 深板岩：19958，20006，20118，20098，19937
     * ---------------------------------------
     * 代码调整前：
     * 黑曜石：15641，16501，16772，16154，16084
     * 切石机：17817，17437，17555，17666，17560
     * 基岩：11527，11506，11488，11375，11324
     * 石头：17397，17194，17470，17326，17319
     * 深板岩：13134，13113，13174，12931，13175
     * ---------------------------------------
     * Set：
     * 黑曜石：23909，24257，23439
     * ---------------------------------------
     * List:
     * 黑曜石：15704，15787，15658
     */

    // 获取方块硬度
    private static int getBlockHardness(CommandContext<ServerCommandSource> context) {
        BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "pos");
        ServerWorld world = context.getSource().getWorld();
        BlockState blockState = world.getBlockState(blockPos);
        long timeMillis = System.currentTimeMillis();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            blockState.getHardness(world, blockPos);
        }
        float hardness = blockState.getHardness(world, blockPos);
        long takeTime = System.currentTimeMillis() - timeMillis;
        MessageUtils.sendMessage(context.getSource(), TextUtils.createText(blockState.getBlock().getName().getString() + "的硬度是" + hardness + "，命令执行耗时" + takeTime + "毫秒"));
        return (int) takeTime;
    }

    // 给予指定方块一个随机刻
    private static int randomTick(CommandContext<ServerCommandSource> context) {
        BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "pos");
        int count = IntegerArgumentType.getInteger(context, "count");
        ServerWorld world = context.getSource().getWorld();
        BlockState blockState = world.getBlockState(blockPos);
        int i;
        for (i = 0; i < count; i++) {
            BlockState worldBlockState = world.getBlockState(blockPos);
            if (blockState.equals(worldBlockState)) {
                worldBlockState.randomTick(world, blockPos, world.getRandom());
                continue;
            }
            break;
        }
        MessageUtils.sendMessage(context.getSource(),
                TextUtils.createText("给予" + blockState.getBlock().getName().getString() + i + "个随机刻"));
        return i;
    }

    // 切换假玩家合成自动更新结果槽
    private static int switchCraftUpdate() {
        // FakePlayerCraftRecipeInterface.flag.setValue(!FakePlayerCraftRecipeInterface.flag.getValue());
        return 1;
    }
}
