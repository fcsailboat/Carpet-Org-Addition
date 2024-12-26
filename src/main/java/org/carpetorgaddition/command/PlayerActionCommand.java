package org.carpetorgaddition.command;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemPredicateArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.fakeplayer.FakePlayerAction;
import org.carpetorgaddition.util.fakeplayer.FakePlayerActionInterface;
import org.carpetorgaddition.util.fakeplayer.FakePlayerActionManager;
import org.carpetorgaddition.util.fakeplayer.actiondata.*;
import org.carpetorgaddition.util.matcher.ItemMatcher;
import org.carpetorgaddition.util.matcher.ItemPredicateMatcher;
import org.carpetorgaddition.util.matcher.Matcher;
import org.carpetorgaddition.util.screen.CraftingSetRecipeScreenHandler;
import org.carpetorgaddition.util.screen.StonecutterSetRecipeScreenHandler;

import java.util.Arrays;
import java.util.function.Predicate;

public class PlayerActionCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext) {
        dispatcher.register(CommandManager.literal("playerAction")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandPlayerAction))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.literal("sorting")
                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .then(CommandManager.argument("this", Vec3ArgumentType.vec3())
                                                .then(CommandManager.argument("other", Vec3ArgumentType.vec3())
                                                        .executes(PlayerActionCommand::setSorting)))))
                        .then(CommandManager.literal("clean")
                                .executes(context -> setClean(context, true))
                                .then(CommandManager.argument("filter", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .executes(context -> setClean(context, false))))
                        .then(CommandManager.literal("fill")
                                .executes(context -> setFIll(context, true))
                                .then(CommandManager.argument("filter", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .executes(context -> setFIll(context, false))))
                        .then(CommandManager.literal("stop")
                                .executes(PlayerActionCommand::setStop))
                        .then(CommandManager.literal("craft")
                                .then(CommandManager.literal("one")
                                        .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                .executes(PlayerActionCommand::setOneCraft)))
                                .then(CommandManager.literal("nine")
                                        .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                .executes(PlayerActionCommand::setNineCraft)))
                                .then(CommandManager.literal("four")
                                        .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                .executes(PlayerActionCommand::setFourCraft)))
                                .then(CommandManager.literal("crafting_table")
                                        .then(registerItemPredicateNode(9, commandBuildContext, PlayerActionCommand::setCraftingTableCraft)))
                                .then(CommandManager.literal("inventory")
                                        .then(registerItemPredicateNode(4, commandBuildContext, PlayerActionCommand::setInventoryCraft)))
                                .then(CommandManager.literal("gui")
                                        .executes(PlayerActionCommand::openFakePlayerCraftGui)))
                        .then(CommandManager.literal("trade")
                                .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> setTrade(context, false))
                                        .then(CommandManager.literal("void_trade")
                                                .executes(context -> setTrade(context, true)))))
                        .then(CommandManager.literal("info")
                                .executes(PlayerActionCommand::getAction))
                        .then(CommandManager.literal("rename")
                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .then(CommandManager.argument("name", StringArgumentType.string())
                                                .executes(PlayerActionCommand::setRename))))
                        .then(CommandManager.literal("stonecutting")
                                .then(CommandManager.literal("item")
                                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                                .then(CommandManager.argument("button", IntegerArgumentType.integer(1))
                                                        .executes(PlayerActionCommand::setStonecutting))))
                                .then(CommandManager.literal("gui")
                                        .executes(PlayerActionCommand::useGuiSetStonecutting)))
                        .then(CommandManager.literal("fishing")
                                .executes(PlayerActionCommand::setFishing))));
    }

    // 注册物品谓词节点
    private static RequiredArgumentBuilder<ServerCommandSource, ItemPredicateArgumentType.ItemStackPredicateArgument>
    registerItemPredicateNode(int maxValue, CommandRegistryAccess commandBuildContext, Command<ServerCommandSource> function) {
        RequiredArgumentBuilder<ServerCommandSource, ItemPredicateArgumentType.ItemStackPredicateArgument> result = null;
        for (int i = maxValue; i >= 1; i--) {
            RequiredArgumentBuilder<ServerCommandSource, ItemPredicateArgumentType.ItemStackPredicateArgument> nobe
                    = CommandManager.argument("item" + i, ItemPredicateArgumentType.itemPredicate(commandBuildContext));
            if (result == null) {
                result = nobe.executes(function);
            } else {
                nobe.then(result);
                result = nobe;
            }
        }
        return result;
    }

    // 设置停止
    private static int setStop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        actionManager.stop();
        return 1;
    }

    // 设置物品分拣
    private static int setSorting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        //获取要分拣的物品对象
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        //获取分拣物品要丢出的方向
        Vec3d thisVec = Vec3ArgumentType.getVec3(context, "this");
        //获取非分拣物品要丢出的方向
        Vec3d otherVec = Vec3ArgumentType.getVec3(context, "other");
        actionManager.setAction(FakePlayerAction.SORTING, new SortingData(item, thisVec, otherVec));
        return 1;
    }

    // 设置清空潜影盒
    private static int setClean(CommandContext<ServerCommandSource> context, boolean allItem) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        if (allItem) {
            // 设置清空潜影盒内的所有物品，不需要获取Item对象
            actionManager.setAction(FakePlayerAction.CLEAN, CleanData.CLEAN_ALL);
        } else {
            Item item = ItemStackArgumentType.getItemStackArgument(context, "filter").getItem();
            actionManager.setAction(FakePlayerAction.CLEAN, new CleanData(item, false));
        }
        return 1;
    }

    // 设置填充潜影盒
    private static int setFIll(CommandContext<ServerCommandSource> context, boolean allItem) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        if (allItem) {
            // 向潜影盒内填充任意物品
            actionManager.setAction(FakePlayerAction.FILL, FillData.FILL_ALL);
        } else {
            Item item = ItemStackArgumentType.getItemStackArgument(context, "filter").getItem();
            actionManager.setAction(FakePlayerAction.FILL, new FillData(item, false));
        }
        return 1;
    }

    // 单个物品合成
    private static int setOneCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        Predicate<ItemStack> item = ItemPredicateArgumentType.getItemStackPredicate(context, "item");
        actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, new InventoryCraftData(fillArray(new ItemPredicateMatcher(item), new Matcher[4], false)));
        return 1;
    }

    // 四个物品合成
    private static int setFourCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        Predicate<ItemStack> item = ItemPredicateArgumentType.getItemStackPredicate(context, "item");
        actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, new InventoryCraftData(fillArray(new ItemPredicateMatcher(item), new Matcher[4], true)));
        return 1;
    }

    // 设置物品栏合成
    private static int setInventoryCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        ItemPredicateMatcher[] items = new ItemPredicateMatcher[4];
        for (int i = 1; i <= 4; i++) {
            // 获取每一个合成材料
            items[i - 1] = new ItemPredicateMatcher(ItemPredicateArgumentType.getItemStackPredicate(context, "item" + i));
        }
        actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, new InventoryCraftData(items));
        return 1;
    }

    // 九个物品合成
    private static int setNineCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        Predicate<ItemStack> item = ItemPredicateArgumentType.getItemStackPredicate(context, "item");
        actionManager.setAction(FakePlayerAction.CRAFTING_TABLE_CRAFT, new CraftingTableCraftData(fillArray(new ItemPredicateMatcher(item), new Matcher[9], true)));
        return 1;
    }

    // 设置工作台合成
    private static int setCraftingTableCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        ItemPredicateMatcher[] items = new ItemPredicateMatcher[9];
        for (int i = 1; i <= 9; i++) {
            items[i - 1] = new ItemPredicateMatcher(ItemPredicateArgumentType.getItemStackPredicate(context, "item" + i));
        }
        actionManager.setAction(FakePlayerAction.CRAFTING_TABLE_CRAFT, new CraftingTableCraftData(items));
        return 1;
    }

    // 设置交易
    private static int setTrade(CommandContext<ServerCommandSource> context, boolean voidTrade) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        // 获取按钮的索引，减去1
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        actionManager.setAction(FakePlayerAction.TRADE, new TradeData(index, voidTrade));
        return 1;
    }

    // 设置重命名
    private static int setRename(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        // 获取当前要操作的物品和要重命名的字符串
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        String newName = StringArgumentType.getString(context, "name");
        actionManager.setAction(FakePlayerAction.RENAME, new RenameData(item, newName));
        return 1;
    }

    // 设置使用切石机
    private static int setStonecutting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        // 获取要切割的物品和按钮的索引
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        int buttonIndex = IntegerArgumentType.getInteger(context, "button") - 1;
        actionManager.setAction(FakePlayerAction.STONECUTTING, new StonecuttingData(item, buttonIndex));
        return 1;
    }

    // 使用GUI设置使用切石机
    private static int useGuiSetStonecutting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, inventory, playerEntity) -> {
            ScreenHandlerContext screenHandlerContext = ScreenHandlerContext.create(player.getWorld(), player.getBlockPos());
            return new StonecutterSetRecipeScreenHandler(i, inventory, screenHandlerContext, fakePlayer);
        }, TextUtils.translate("carpet.commands.playerAction.info.stonecutter.gui"));
        player.openHandledScreen(screen);
        return 1;
    }

    // 设置自动钓鱼
    private static int setFishing(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        actionManager.setAction(FakePlayerAction.FISHING, new FishingData());
        return 1;
    }

    // 填充数组
    private static Matcher[] fillArray(Matcher matcher, Matcher[] matchers, boolean directFill) {
        if (directFill) {
            // 直接使用元素填满整个数组
            Arrays.fill(matchers, matcher);
        } else {
            // 第一个元素填入指定物品，其他元素填入空气
            for (int i = 0; i < matchers.length; i++) {
                if (i == 0) {
                    matchers[i] = matcher;
                } else {
                    matchers[i] = ItemMatcher.AIR_ITEM_MATCHER;
                }
            }
        }
        return matchers;
    }

    //获取假玩家操作类型
    private static int getAction(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        MessageUtils.sendListMessage(context.getSource(), actionManager.getActionData().info(fakePlayer));
        return 1;
    }

    // 打开控制假人合成物品的GUI
    private static int openFakePlayerCraftGui(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 打开合成GUI
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity)
                -> new CraftingSetRecipeScreenHandler(i, playerInventory, fakePlayer,
                ScreenHandlerContext.create(player.getWorld(), player.getBlockPos()), context),
                TextUtils.translate("carpet.commands.playerAction.info.craft.gui"));
        player.openHandledScreen(screen);
        return 1;
    }

    // 提示启用Ctrl+Q合成修复
    public static void promptToEnableCtrlQCraftingFix(ServerCommandSource source) {
        if (CarpetSettings.ctrlQCraftingFix) {
            return;
        }
        String command = "/carpet ctrlQCraftingFix true";
        // [这里]的悬停提示
        MutableText hoverText = TextConstants.clickInput(command);
        MutableText suggest = TextUtils.suggest(TextConstants.CLICK_HERE.copy(), command, hoverText, Formatting.AQUA);
        MessageUtils.sendMessage(source, "carpet.commands.playerAction.set", suggest);
    }

    // 在设置假玩家合成时获取动作管理器并提示启用合成修复
    private static FakePlayerActionManager prepareTheCrafting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 提示启用合成修复
        promptToEnableCtrlQCraftingFix(context.getSource());
        return FakePlayerActionInterface.getManager(fakePlayer);
    }
}
