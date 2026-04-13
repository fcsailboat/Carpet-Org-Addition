package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.config.GlobalConfigs;
import boat.carpetorgaddition.periodic.FakePlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.action.*;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.permission.CommandPermission;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.screen.CraftingSetRecipeScreenHandler;
import boat.carpetorgaddition.wheel.screen.StonecutterSetRecipeScreenHandler;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class PlayerActionCommand extends AbstractServerCommand {
    private static final CommandPermission AI_PERMISSION = PermissionManager.registerHiddenCommand("playerAction.player.bedrock.ai", PermissionLevel.PASS);
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("playerAction");

    public PlayerActionCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.COMMAND_PLAYER_ACTION))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(thenSorting())
                        .then(Commands.literal("empty")
                                .executes(context -> setEmptyTheContainer(context, true))
                                .then(Commands.argument("filter", ItemPredicateArgument.itemPredicate(this.access))
                                        .executes(context -> setEmptyTheContainer(context, false))))
                        .then(Commands.literal("fill")
                                .executes(context -> setFillTheContainer(context, true, true, false))
                                .then(Commands.argument("filter", ItemPredicateArgument.itemPredicate(this.access))
                                        .executes(context -> setFillTheContainer(context, false, true, false))
                                        .then(Commands.argument(FillTheContainerAction.DROP_OTHER, BoolArgumentType.bool())
                                                .executes(context -> setFillTheContainer(context, false, BoolArgumentType.getBool(context, FillTheContainerAction.DROP_OTHER), false))
                                                .then(Commands.argument(FillTheContainerAction.MORE_CONTAINER, BoolArgumentType.bool())
                                                        .executes(context -> setFillTheContainer(context, false, BoolArgumentType.getBool(context, FillTheContainerAction.DROP_OTHER), BoolArgumentType.getBool(context, FillTheContainerAction.MORE_CONTAINER)))))))
                        .then(Commands.literal("stop")
                                .executes(this::setStop))
                        .then(Commands.literal("craft")
                                .then(Commands.literal("one")
                                        .then(Commands.argument("item", ItemPredicateArgument.itemPredicate(this.access))
                                                .executes(this::setOneCraft)))
                                .then(Commands.literal("nine")
                                        .then(Commands.argument("item", ItemPredicateArgument.itemPredicate(this.access))
                                                .executes(this::setNineCraft)))
                                .then(Commands.literal("four")
                                        .then(Commands.argument("item", ItemPredicateArgument.itemPredicate(this.access))
                                                .executes(this::setFourCraft)))
                                .then(Commands.literal("crafting_table")
                                        .then(registerItemPredicateNode(9, this.access, this::setCraftingTableCraft)))
                                .then(Commands.literal("inventory")
                                        .then(registerItemPredicateNode(4, this.access, this::setInventoryCraft)))
                                .then(Commands.literal("gui")
                                        .executes(this::openFakePlayerCraftGui)))
                        .then(Commands.literal("trade")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> setTrade(context, false))
                                        .then(Commands.literal("void_trade")
                                                .executes(context -> setTrade(context, true)))))
                        .then(Commands.literal("info")
                                .executes(this::getAction))
                        .then(Commands.literal("rename")
                                .then(Commands.argument("item", ItemArgument.item(this.access))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .executes(this::setRename))))
                        .then(Commands.literal("stonecutting")
                                .then(Commands.literal("item")
                                        .then(Commands.argument("item", ItemPredicateArgument.itemPredicate(this.access))
                                                .then(Commands.argument("button", IntegerArgumentType.integer(1))
                                                        .executes(this::setStonecutting))))
                                .then(Commands.literal("gui")
                                        .executes(this::useGuiSetStonecutting)))
                        .then(Commands.literal("fishing")
                                .executes(this::setFishing))
                        .then(Commands.literal("plant")
                                .requires(_ -> CarpetOrgAdditionConstants.isEnableHiddenFunction())
                                .executes(this::setPlant))
                        .then(register(Commands.literal("bedrock")
                                .requires(_ -> CarpetOrgAdditionConstants.isEnableHiddenFunction())))
                        .then(Commands.literal("goto")
                                .requires(_ -> CarpetOrgAdditionConstants.isEnableHiddenFunction())
                                .then(Commands.literal("block")
                                        .then(Commands.argument("target", BlockPosArgument.blockPos())
                                                .executes(this::setGotoBlockPos)))
                                .then(Commands.literal("entity")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(this::setGotoEntity))))
                        .then(Commands.literal("raise")
                                .requires(_ -> CarpetOrgAddition.isDebugMode())
                                .executes(context -> this.raise(context, null))
                                .then(Commands.argument("message", StringArgumentType.string())
                                        .executes(context -> this.raise(context, StringArgumentType.getString(context, "message")))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> thenSorting() {
        LiteralArgumentBuilder<CommandSourceStack> sorting = Commands.literal("sorting");
        RequiredArgumentBuilder<CommandSourceStack, Result> result = null;
        int max = GlobalConfigs.getInstance().getPlayerActionMaxSortingItemCount();
        for (int i = max; i >= 0; i--) {
            final int count = i + 1;
            var argument = Commands.argument("item" + count, ItemPredicateArgument.itemPredicate(this.access));
            if (result != null) {
                if (i < max - 1) {
                    argument = argument.then(Commands.literal("or").then(result));
                }
                argument.then(Commands.literal("at")
                        .then(Commands.argument("this", Vec3Argument.vec3())
                                .then(Commands.argument("other", Vec3Argument.vec3())
                                        .executes(context -> setCategorize(context, count)))));
            }
            result = argument;
        }
        return sorting.then(result);
    }

    private LiteralArgumentBuilder<CommandSourceStack> register(LiteralArgumentBuilder<CommandSourceStack> node) {
        for (BedrockAction.BedrockRegionType value : BedrockAction.BedrockRegionType.values()) {
            node.then(Commands.literal(value.name().toLowerCase(Locale.ROOT))
                    .then(register(value).apply(
                            Commands.argument("ai", BoolArgumentType.bool())
                                    .requires(AI_PERMISSION)
                                    .executes(context -> {
                                        boolean ai = BoolArgumentType.getBool(context, "ai");
                                        return setBreakBedrock(context, value, ai, false);
                                    })
                                    .then(Commands.argument("timedMaterialRecycling", BoolArgumentType.bool())
                                            .executes(context -> {
                                                boolean ai = BoolArgumentType.getBool(context, "ai");
                                                boolean timedMaterialRecycling = BoolArgumentType.getBool(context, "timedMaterialRecycling");
                                                return setBreakBedrock(context, value, ai, timedMaterialRecycling);
                                            })))));
        }
        return node;
    }

    private Function<RequiredArgumentBuilder<CommandSourceStack, ?>, RequiredArgumentBuilder<CommandSourceStack, ?>> register(BedrockAction.BedrockRegionType value) {
        Command<CommandSourceStack> command = context -> setBreakBedrock(context, value, false, false);
        return switch (value) {
            case CUBOID -> argument ->
                    Commands.argument("from", BlockPosArgument.blockPos())
                            .then(Commands.argument("to", BlockPosArgument.blockPos())
                                    .executes(command)
                                    .then(argument));
            case CYLINDER -> argument ->
                    Commands.argument("center", BlockPosArgument.blockPos())
                            .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                                    .then(Commands.argument("height", IntegerArgumentType.integer(1, 1024))
                                            .executes(command)
                                            .then(argument)));
        };
    }

    // 注册物品谓词节点
    private RequiredArgumentBuilder<CommandSourceStack, Result> registerItemPredicateNode(
            int maxValue,
            CommandBuildContext access,
            Command<CommandSourceStack> function
    ) {
        RequiredArgumentBuilder<CommandSourceStack, Result> result = null;
        for (int i = maxValue; i >= 1; i--) {
            var nobe = Commands.argument("item" + i, ItemPredicateArgument.itemPredicate(access));
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
    private int setStop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        actionManager.stop();
        return 1;
    }

    // 设置物品分拣
    private int setCategorize(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        List<ItemStackPredicate> predicates = new ArrayList<>();
        // 获取要分拣的物品
        for (int i = 1; i <= count; i++) {
            predicates.add(new ItemStackPredicate(context, "item" + i));
        }
        // 获取分拣物品要丢出的方向
        Vec3 thisVec = Vec3Argument.getVec3(context, "this");
        // 获取非分拣物品要丢出的方向
        Vec3 otherVec = Vec3Argument.getVec3(context, "other");
        actionManager.setAction(new ItemCategorizeAction(fakePlayer, predicates, thisVec, otherVec));
        return 1;
    }

    // 设置清空容器
    private int setEmptyTheContainer(CommandContext<CommandSourceStack> context, boolean allItem) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        ItemStackPredicate predicate = allItem ? ItemStackPredicate.WILDCARD : new ItemStackPredicate(context, "filter");
        actionManager.setAction(new EmptyTheContainerAction(fakePlayer, predicate));
        return 1;
    }

    // 设置填充容器
    private int setFillTheContainer(CommandContext<CommandSourceStack> context, boolean allItem, boolean dropOther, boolean moreContainer) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        ItemStackPredicate predicate = allItem ? ItemStackPredicate.WILDCARD : new ItemStackPredicate(context, "filter");
        actionManager.setAction(new FillTheContainerAction(fakePlayer, predicate, dropOther, moreContainer));
        return 1;
    }

    // 单个物品合成
    private int setOneCraft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        ItemStackPredicate[] predicates = fillArray(predicate, new ItemStackPredicate[4], false);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        actionManager.setAction(new InventoryCraftAction(fakePlayer, predicates));
        return 1;
    }

    // 四个物品合成
    private int setFourCraft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        ItemStackPredicate[] predicates = fillArray(predicate, new ItemStackPredicate[4], true);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        actionManager.setAction(new InventoryCraftAction(fakePlayer, predicates));
        return 1;
    }

    // 设置物品栏合成
    private int setInventoryCraft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        ItemStackPredicate[] items = new ItemStackPredicate[4];
        for (int i = 1; i <= 4; i++) {
            // 获取每一个合成材料
            items[i - 1] = new ItemStackPredicate(context, "item" + i);
        }
        actionManager.setAction(new InventoryCraftAction(fakePlayer, items));
        return 1;
    }

    // 九个物品合成
    private int setNineCraft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        ItemStackPredicate[] predicates = fillArray(predicate, new ItemStackPredicate[9], true);
        actionManager.setAction(new CraftingTableCraftAction(fakePlayer, predicates));
        return 1;
    }

    // 设置工作台合成
    private int setCraftingTableCraft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        ItemStackPredicate[] items = new ItemStackPredicate[9];
        for (int i = 1; i <= 9; i++) {
            items[i - 1] = new ItemStackPredicate(context, "item" + i);
        }
        actionManager.setAction(new CraftingTableCraftAction(fakePlayer, items));
        return 1;
    }

    // 设置交易
    private int setTrade(CommandContext<CommandSourceStack> context, boolean voidTrade) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        // 获取按钮的索引，减去1
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        actionManager.setAction(new TradeAction(fakePlayer, index, voidTrade));
        return 1;
    }

    // 设置重命名
    private int setRename(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        // 获取当前要操作的物品和要重命名的字符串
        Item item = ItemArgument.getItem(context, "item").item().value();
        String newName = StringArgumentType.getString(context, "name");
        actionManager.setAction(new RenameAction(fakePlayer, item, newName));
        return 1;
    }

    // 设置使用切石机
    private int setStonecutting(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        // 获取要切割的物品和按钮的索引
        int buttonIndex = IntegerArgumentType.getInteger(context, "button") - 1;
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        actionManager.setAction(new StonecuttingAction(fakePlayer, predicate, buttonIndex));
        return 1;
    }

    // 使用GUI设置使用切石机
    private int useGuiSetStonecutting(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        PlayerUtils.openScreenHandler(player, (syncId, inventory, _) -> new StonecutterSetRecipeScreenHandler(
                        syncId,
                        inventory,
                        ContainerLevelAccess.create(ServerUtils.getWorld(player), player.blockPosition()),
                        fakePlayer),
                StonecuttingAction.KEY.then("gui").translate()
        );
        return 1;
    }

    // 设置自动钓鱼
    private int setFishing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        actionManager.setAction(new FishingAction(fakePlayer));
        return 1;
    }

    // 设置自动种植
    private int setPlant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
            FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
            actionManager.setAction(new PlantAction(fakePlayer));
            return 1;
        }
        return 0;
    }

    // 设置破基岩
    private int setBreakBedrock(CommandContext<CommandSourceStack> context, BedrockAction.BedrockRegionType regionType, boolean ai, boolean timedMaterialRecycling) throws CommandSyntaxException {
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            BedrockAction action = switch (regionType) {
                case CUBOID -> {
                    BlockPos from = BlockPosArgument.getBlockPos(context, "from");
                    BlockPos to = BlockPosArgument.getBlockPos(context, "to");
                    yield new BedrockAction(fakePlayer, from, to, ai, timedMaterialRecycling);
                }
                case CYLINDER -> {
                    BlockPos center = BlockPosArgument.getBlockPos(context, "center");
                    int radius = IntegerArgumentType.getInteger(context, "radius");
                    int height = IntegerArgumentType.getInteger(context, "height");
                    yield new BedrockAction(fakePlayer, center, radius, height, ai, timedMaterialRecycling);
                }
            };
            FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
            FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
            actionManager.setAction(action);
            Optional<ServerPlayer> optional = CommandUtils.getSourcePlayerNullable(context);
            if (optional.isPresent()) {
                Component translate = BedrockAction.KEY.then("share").translate();
                MessageUtils.sendMessageToHud(optional.get(), translate);
            }
            return 1;
        }
        return 0;
    }

    // 设置寻路到方块
    private int setGotoBlockPos(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            BlockPos target = BlockPosArgument.getBlockPos(context, "target");
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
            FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
            actionManager.setAction(new GotoAction(fakePlayer, target));
            return 1;
        }
        return 0;
    }

    // 设置寻路到实体
    private int setGotoEntity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            Entity target = EntityArgument.getEntity(context, "target");
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
            FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
            actionManager.setAction(new GotoAction(fakePlayer, target));
            return 1;
        }
        return 0;
    }

    // 填充数组
    private ItemStackPredicate[] fillArray(ItemStackPredicate matcher, ItemStackPredicate[] matchers, boolean directFill) {
        if (directFill) {
            // 直接使用元素填满整个数组
            Arrays.fill(matchers, matcher);
        } else {
            // 第一个元素填入指定物品，其他元素填入空气
            for (int i = 0; i < matchers.length; i++) {
                if (i == 0) {
                    matchers[i] = matcher;
                } else {
                    matchers[i] = ItemStackPredicate.EMPTY;
                }
            }
        }
        return matchers;
    }

    // 获取假玩家操作类型
    private int getAction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        AbstractPlayerAction action = actionManager.getAction();
        if (action.equalFakePlayer(null)) {
            action.setFakePlayer(fakePlayer);
        }
        MessageUtils.sendListMessage(context.getSource(), action.info());
        return 1;
    }

    // 打开控制假人合成物品的GUI
    private int openFakePlayerCraftGui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 打开合成GUI
        PlayerUtils.openScreenHandler(player,
                (syncId, playerInventory, _) -> new CraftingSetRecipeScreenHandler(
                        syncId,
                        playerInventory,
                        fakePlayer,
                        ContainerLevelAccess.create(ServerUtils.getWorld(player), player.blockPosition())
                ),
                CraftingTableCraftAction.KEY.then("gui").translate()
        );
        return 1;
    }

    // 调试：设置动作抛出异常
    private int raise(CommandContext<CommandSourceStack> context, @Nullable String message) throws CommandSyntaxException {
        if (CarpetOrgAddition.isDebugMode()) {
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(fakePlayer);
            FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
            actionManager.setDebugExceptionMessage(message == null ? "Manually triggered debug exception" : message);
            return 1;
        }
        return 0;
    }

    @Override
    public String getDefaultName() {
        return "playerAction";
    }
}
