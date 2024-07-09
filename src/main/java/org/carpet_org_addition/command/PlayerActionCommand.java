package org.carpet_org_addition.command;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import org.carpet_org_addition.CarpetOrgAdditionSettings;
import org.carpet_org_addition.util.CommandUtils;
import org.carpet_org_addition.util.MessageUtils;
import org.carpet_org_addition.util.TextUtils;
import org.carpet_org_addition.util.fakeplayer.CraftingSetRecipeScreenHandler;
import org.carpet_org_addition.util.fakeplayer.FakePlayerAction;
import org.carpet_org_addition.util.fakeplayer.FakePlayerActionInterface;
import org.carpet_org_addition.util.fakeplayer.FakePlayerActionManager;
import org.carpet_org_addition.util.fakeplayer.actiondata.*;
import org.carpet_org_addition.util.matcher.ItemMatcher;
import org.carpet_org_addition.util.matcher.ItemPredicateMatcher;
import org.carpet_org_addition.util.matcher.Matcher;

import java.util.Arrays;
import java.util.function.Predicate;

public class PlayerActionCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext) {
        dispatcher.register(CommandManager.literal("playerAction").requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandPlayerAction))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.literal("sorting")
                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .then(CommandManager.argument("this", Vec3ArgumentType.vec3())
                                                .then(CommandManager.argument("other", Vec3ArgumentType.vec3())
                                                        .executes(PlayerActionCommand::setSorting)))))
                        .then(CommandManager.literal("clean")
                                .executes(context -> setClean(context, true))
                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .executes(context -> setClean(context, false))))
                        .then(CommandManager.literal("fill")
                                .executes(context -> setFIll(context, true))
                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .executes(context -> setFIll(context, false))))
                        .then(CommandManager.literal("stop").executes(PlayerActionCommand::setStop))
                        .then(CommandManager.literal("craft")
                                .then(CommandManager.literal("one").then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                        .executes(PlayerActionCommand::setOneCraft)))
                                .then(CommandManager.literal("nine").then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                        .executes(PlayerActionCommand::setNineCraft)))
                                .then(CommandManager.literal("four").then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                        .executes(PlayerActionCommand::setFourCraft)))
                                .then(CommandManager.literal("3x3")
                                        // TODO 使用循环for优化代码
                                        .then(CommandManager.argument("item1", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                .then(CommandManager.argument("item2", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                        .then(CommandManager.argument("item3", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                .then(CommandManager.argument("item4", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                        .then(CommandManager.argument("item5", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                                .then(CommandManager.argument("item6", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                                        .then(CommandManager.argument("item7", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                                                .then(CommandManager.argument("item8", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                                                        .then(CommandManager.argument("item9", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                                                                .executes(PlayerActionCommand::setCraftingTableCraft)))))))))))
                                .then(CommandManager.literal("2x2")
                                        .then(CommandManager.argument("item1", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                .then(CommandManager.argument("item2", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                        .then(CommandManager.argument("item3", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                .then(CommandManager.argument("item4", ItemPredicateArgumentType.itemPredicate(commandBuildContext))
                                                                        .executes(PlayerActionCommand::setInventoryCraft))))))
                                .then(CommandManager.literal("gui").executes(PlayerActionCommand::openFakePlayerCraftGui)))
                        .then(CommandManager.literal("trade")
                                .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> setTrade(context, false))
                                        .then(CommandManager.literal("void_trade")
                                                .executes(context -> setTrade(context, true)))))
                        .then(CommandManager.literal("info").executes(PlayerActionCommand::getAction))
                        .then(CommandManager.literal("rename").then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(PlayerActionCommand::setRename))))
                        .then(CommandManager.literal("stonecutting").then(CommandManager.argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                                .then(CommandManager.argument("button", IntegerArgumentType.integer(1))
                                        .executes(PlayerActionCommand::setStonecutting))))));
    }

    // 设置停止
    private static int setStop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        actionManager.stop();
        return 0;
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
        return 8;
    }

    // 设置清空潜影盒
    private static int setClean(CommandContext<ServerCommandSource> context, boolean allItem) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        if (allItem) {
            // 设置清空潜影盒内的所有物品，不需要获取Item对象
            actionManager.setAction(FakePlayerAction.CLEAN, CleanData.CLEAN_ALL);
        } else {
            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
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
            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
            actionManager.setAction(FakePlayerAction.FILL, new FillData(item, false));
        }
        return 5;
    }

    // 单个物品合成
    private static int setOneCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = getActionManager(context);
        Predicate<ItemStack> item = ItemPredicateArgumentType.getItemStackPredicate(context, "item");
        actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, new InventoryCraftData(fillArray(new ItemPredicateMatcher(item), new Matcher[4], false)));
        return 2;
    }

    // 四个物品合成
    private static int setFourCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = getActionManager(context);
        Predicate<ItemStack> item = ItemPredicateArgumentType.getItemStackPredicate(context, "item");
        actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, new InventoryCraftData(fillArray(new ItemPredicateMatcher(item), new Matcher[4], true)));
        return 2;
    }

    // 设置物品栏合成
    private static int setInventoryCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = getActionManager(context);
        ItemPredicateMatcher[] items = new ItemPredicateMatcher[4];
        for (int i = 1; i <= 4; i++) {
            // 获取每一个合成材料
            items[i - 1] = new ItemPredicateMatcher(ItemPredicateArgumentType.getItemStackPredicate(context, "item" + i));
        }
        actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, new InventoryCraftData(items));
        return 2;
    }

    // 九个物品合成
    private static int setNineCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = getActionManager(context);
        Predicate<ItemStack> item = ItemPredicateArgumentType.getItemStackPredicate(context, "item");
        actionManager.setAction(FakePlayerAction.CRAFTING_TABLE_CRAFT, new CraftingTableCraftData(fillArray(new ItemPredicateMatcher(item), new Matcher[9], true)));
        return 3;
    }

    // 设置工作台合成
    private static int setCraftingTableCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FakePlayerActionManager actionManager = getActionManager(context);
        ItemPredicateMatcher[] items = new ItemPredicateMatcher[9];
        for (int i = 1; i <= 9; i++) {
            items[i - 1] = new ItemPredicateMatcher(ItemPredicateArgumentType.getItemStackPredicate(context, "item" + i));
        }
        actionManager.setAction(FakePlayerAction.CRAFTING_TABLE_CRAFT, new CraftingTableCraftData(items));
        return 3;
    }

    // 设置交易
    private static int setTrade(CommandContext<ServerCommandSource> context, boolean voidTrade) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        // 获取按钮的索引，减去1
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        actionManager.setAction(FakePlayerAction.TRADE, new TradeData(index, voidTrade));
        return 10;
    }

    // 设置重命名
    private static int setRename(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        // 获取当前要操作的物品和要重命名的字符串
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        String newName = StringArgumentType.getString(context, "name");
        actionManager.setAction(FakePlayerAction.RENAME, new RenameData(item, newName));
        return 7;
    }

    // 设置使用切石机
    private static int setStonecutting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        // 获取要切割的物品和按钮的索引
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        int buttonIndex = IntegerArgumentType.getInteger(context, "button") - 1;
        actionManager.setAction(FakePlayerAction.STONECUTTING, new StonecuttingData(item, buttonIndex));
        return 9;
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
        return 6;
    }

    // 打开控制假人合成物品的GUI
    private static int openFakePlayerCraftGui(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 打开合成GUI
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity)
                -> new CraftingSetRecipeScreenHandler(i, playerInventory, fakePlayer,
                ScreenHandlerContext.create(player.getWorld(), player.getBlockPos()), context),
                TextUtils.getTranslate("carpet.commands.playerAction.info.craft.gui"));
        player.openHandledScreen(screen);
        return 4;
    }

    // 提示启用Ctrl+Q合成修复
    public static void promptToEnableCtrlQCraftingFix(ServerCommandSource source) {
        if (CarpetSettings.ctrlQCraftingFix) {
            return;
        }
        //判断当前命令执行者是否有足够的权限
        boolean hasPermission = source.hasPermissionLevel(getCarpetPermissionLevel(source));
        MutableText suggest;
        if (hasPermission) {
            suggest = TextUtils.suggest(TextUtils.getTranslate("carpet.commands.playerAction.set.here")
                            .getString(), "/carpet ctrlQCraftingFix true",
                    TextUtils.getTranslate("carpet.commands.playerAction.set.has_permission"),
                    Formatting.AQUA);
        } else {
            suggest = TextUtils.suggest(
                    TextUtils.getTranslate("carpet.commands.playerAction.set.here").getString(),
                    null, TextUtils.getTranslate("carpet.commands.playerAction.set.no_permission"),
                    Formatting.RED);
        }
        MessageUtils.sendCommandFeedback(source, "carpet.commands.playerAction.set", suggest);
    }

    // 在设置假玩家合成时获取动作管理器并提示启用合成修复
    private static FakePlayerActionManager getActionManager(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        promptToEnableCtrlQCraftingFix(context.getSource());
        return actionManager;
    }

    //获取执行carpet命令需要的权限等级
    private static int getCarpetPermissionLevel(ServerCommandSource source) {
        if (CarpetOrgAdditionSettings.openCarpetPermissions && source.getServer().isSingleplayer()) {
            return 0;
        }
        if ("4".equals(CarpetSettings.carpetCommandPermissionLevel)) {
            return 4;
        }
        return 2;
    }
}
