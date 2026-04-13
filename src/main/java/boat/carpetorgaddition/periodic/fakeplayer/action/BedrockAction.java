package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.exception.InfiniteLoopException;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerPathfinder;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.SimpleCounter;
import boat.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import boat.carpetorgaddition.wheel.traverser.CylinderBlockPosTraverser;
import boat.carpetorgaddition.wheel.traverser.EntityTraverser;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BedrockAction extends AbstractPlayerAction {
    private final LinkedHashSet<BedrockBreakingContext> contexts = new LinkedHashSet<>();
    private final HashSet<BlockPos> lavas = new HashSet<>();
    private final BlockPosTraverser traverser;
    private final BedrockRegionType regionType;
    private PlayerStorageInventory inventory;
    private BlockExcavator excavator;
    @NonNull
    private FakePlayerPathfinder pathfinder = FakePlayerPathfinder.EMPTY;
    /**
     * 玩家是否有AI，是否可以自动寻路，自动进食
     */
    private final boolean ai;
    /**
     * 下一次定时回收材料的剩余游戏刻数，剩余时间为-1表示禁用定时回收
     */
    private int recycleTimer;
    @Nullable
    private BedrockBreakingContext currentContext;
    @NonNull
    private PlayerWorkPhase phase = PlayerWorkPhase.WORK;
    /**
     * 玩家上一个任务阶段
     */
    @NonNull
    private PlayerWorkPhase prevPhase = getPhase();
    /**
     * 玩家当前的移动目标
     */
    @Nullable
    private BlockPos bedrockTarget;
    /**
     * 距离最近的物品实体
     */
    @Nullable
    private ItemEntity recentItemEntity;
    /**
     * 正在移动到近处的基岩
     */
    private boolean isMovingToNearbyBedrock = false;
    /**
     * 当前游戏刻玩家是否执行了破基岩的动作，例如放置活塞，点击拉杆等
     */
    private boolean hasAction;
    /**
     * 是否刚刚完成材料收集，用于在开始破基岩时清除之前收集材料的寻路
     */
    private boolean materialCollectionComplete = false;
    /**
     * 因处于特殊位置而无法破除的基岩
     */
    private final SimpleCounter<BlockPos> invalidBedrock = new SimpleCounter<>();
    /**
     * 周围的材料物品
     */
    private final ArrayList<ItemEntity> itemEntities = new ArrayList<>();
    /**
     * 定时回收材料的时间间隔（3分钟）
     */
    private static final int MATERIAL_RECYCLING_TIME = 3600;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("bedrock");

    private BedrockAction(EntityPlayerMPFake fakePlayer, BlockPosTraverser traverser, BedrockRegionType regionType, boolean ai, boolean timedMaterialRecycling) {
        super(fakePlayer);
        this.traverser = traverser;
        this.regionType = regionType;
        this.ai = ai;
        this.recycleTimer = timedMaterialRecycling ? MATERIAL_RECYCLING_TIME : -1;
    }

    public BedrockAction(EntityPlayerMPFake fakePlayer, BlockPos from, BlockPos to, boolean ai, boolean timedMaterialRecycling) {
        this(fakePlayer, new BlockPosTraverser(from, to), BedrockRegionType.CUBOID, ai, timedMaterialRecycling);
    }

    public BedrockAction(EntityPlayerMPFake fakePlayer, BlockPos center, int radius, int height, boolean ai, boolean timedMaterialRecycling) {
        this(fakePlayer, new CylinderBlockPosTraverser(center, radius, height), BedrockRegionType.CYLINDER, ai, timedMaterialRecycling);
    }

    @Override
    protected void tick() {
        if (this.ai) {
            this.pathfinder.tick();
            // 周围有下落的方块，暂停寻路，防止被砸死
            if (this.hasFallingSand(3)) {
                this.pathfinder.pause(3);
            }
            this.itemEntities.removeIf(Entity::isRemoved);
            if (this.recentItemEntity != null && this.recentItemEntity.isRemoved()) {
                this.recentItemEntity = null;
            }
            if (shouldEat()) {
                if (this.getPhase() != PlayerWorkPhase.EAT) {
                    this.prevPhase = this.getPhase();
                }
                this.setPhase(PlayerWorkPhase.EAT);
            } else if (this.recycleTimer > 0) {
                this.recycleTimer--;
                if (this.recycleTimer == 0) {
                    this.setPhase(PlayerWorkPhase.COLLECT);
                }
            }
            if (this.recycleTimer < -1) {
                throw new IllegalStateException("The remaining time for material recycling should not be %s".formatted(this.recycleTimer));
            }
            switch (this.getPhase()) {
                case WORK -> work();
                case EAT -> eat();
                case COLLECT -> collectingMaterials();
            }
        } else {
            this.work();
        }
    }

    private void work() {
        this.hasAction = false;
        if (this.materialCollectionComplete) {
            this.materialCollectionComplete = false;
            this.bedrockTarget = null;
        }
        this.drainFluidLava();
        for (BlockPos blockPos : this.invalidBedrock) {
            this.invalidBedrock.decrement(blockPos);
        }
        if (this.tickCurrentWork()) {
            return;
        }
        Level world = ServerUtils.getWorld(this.getFakePlayer());
        this.removeIf(context -> {
            if (context.getState() == BreakingState.COMPLETE) {
                return true;
            }
            if (world.getBlockState(context.getBedrockPos()).is(Blocks.BEDROCK)) {
                return !canInteract(context.getBedrockPos());
            }
            return context.getState() != BreakingState.CLEAN_PISTON;
        });
        double range = this.getFakePlayer().blockInteractionRange();
        // 如果this.selectionArea过大，遍历时可能会造成大量卡顿
        AABB box = new AABB(this.getFakePlayer().blockPosition()).inflate(Math.min(range, 10.0));
        for (BlockPos blockPos : new BlockPosTraverser(box)) {
            if (canInteract(blockPos) && this.inSelectionArea(blockPos)) {
                BlockState blockState = world.getBlockState(blockPos);
                if (blockState.is(Blocks.BEDROCK)) {
                    this.add(new BedrockBreakingContext(blockPos));
                } else if (blockState.is(Blocks.LAVA)) {
                    this.lavas.add(blockPos);
                }
            }
        }
        for (BedrockBreakingContext context : this.contexts) {
            if (context == this.currentContext) {
                continue;
            }
            if (tickWork(context)) {
                return;
            }
        }
        if (this.ai) {
            this.setGotoTarget(world);
        }
    }

    /**
     * 设置目标基岩位置
     */
    private void setGotoTarget(Level world) {
        if (this.hasAction) {
            if (this.pathfinder.isFinished()) {
                this.bedrockTarget = null;
            }
            return;
        }
        this.isMovingToNearbyBedrock = false;
        Optional<BlockPos> optional = this.getMovingTarget();
        if (optional.isPresent() && this.canInteract(optional.get())) {
            // 基岩在交互距离内，但因位置特殊而无法破除的基岩
            // 重新选择目标位置，并将当前目标位置标记为无效位置
            this.selectRandomBedrock(world);
            this.invalidBedrock.set(optional.get(), 200);
            return;
        }
        for (BedrockBreakingContext context : this.contexts) {
            BlockPos blockPos = context.getBedrockPos();
            if (this.invalidBedrock.getCount(blockPos) > 0) {
                continue;
            }
            this.bedrockTarget = blockPos;
            return;
        }
        this.selectRandomBedrock(world);
    }

    /**
     * 随机选择基岩位置
     */
    private void selectRandomBedrock(Level world) {
        if (this.pathfinder.isInvalid()) {
            for (int i = 0; i < 100; i++) {
                BlockPos blockPos = this.traverser.randomBlockPos();
                if (world.getBlockState(blockPos).is(Blocks.BEDROCK)) {
                    this.bedrockTarget = blockPos;
                    return;
                }
            }
        }
    }

    /**
     * @return 指定方块坐标是否在玩家交互距离内
     */
    private boolean canInteract(BlockPos blockPos) {
        return this.getFakePlayer().isWithinBlockInteractionRange(blockPos, 0.0);
    }

    private boolean tickCurrentWork() {
        if (this.currentContext == null) {
            return false;
        }
        if (this.contexts.contains(this.currentContext)) {
            if (!this.isMovingToNearbyBedrock) {
                this.isMovingToNearbyBedrock = true;
                this.bedrockTarget = this.currentContext.getBedrockPos();
            }
            return this.tickWork(this.currentContext);
        }
        this.currentContext = null;
        return false;
    }

    private boolean tickWork(BedrockBreakingContext context) {
        int loopCount = 0;
        loop:
        while (true) {
            loopCount++;
            if (loopCount > 10) {
                throw new InfiniteLoopException();
            }
            StepResult stepResult = start(context);
            switch (stepResult) {
                case COMPLETION -> {
                    break loop;
                }
                case TICK_COMPLETION -> {
                    this.currentContext = context;
                    return true;
                }
                default -> {
                }
            }
        }
        return false;
    }

    private StepResult start(BedrockBreakingContext context) {
        BlockPos bedrockPos = context.getBedrockPos();
        switch (context.getState()) {
            case PLACE_THE_PISTON_FACING_UP -> {
                if (hasMaterial()) {
                    StepResult stepResult = placePiston(bedrockPos);
                    if (stepResult == StepResult.CONTINUE) {
                        context.nextStep();
                    } else {
                        return stepResult;
                    }
                } else {
                    // 玩家没有足够的材料
                    this.setPhase(PlayerWorkPhase.COLLECT);
                    return StepResult.TICK_COMPLETION;
                }
            }
            case PLACE_AND_ACTIVATE_THE_LEVER -> {
                StepResult stepResult = placeAndActivateTheLever(context);
                if (stepResult == StepResult.CONTINUE) {
                    context.nextStep();
                } else {
                    return stepResult;
                }
                // 不管是否成功放置都结束方法
                return StepResult.COMPLETION;
            }
            case PISTON_BREAK_BEDROCK -> {
                StepResult stepResult = pistonBreakBedrock(context);
                switch (stepResult) {
                    // 基岩破除，结束当前位置
                    case COMPLETION -> {
                        context.nextStep();
                        return StepResult.COMPLETION;
                    }
                    // 活塞没有挖掘完毕，结束当前tick
                    case TICK_COMPLETION -> {
                        return StepResult.TICK_COMPLETION;
                    }
                    case FAIL -> {
                        context.fail();
                        return StepResult.COMPLETION;
                    }
                    default -> throw new IllegalStateException();
                }
            }
            case CLEAN_PISTON -> {
                if (cleanPiston(bedrockPos.above())) {
                    // 活塞挖掘完毕，执行下一步
                    context.nextStep();
                } else {
                    return StepResult.TICK_COMPLETION;
                }
            }
            default -> {
                return StepResult.COMPLETION;
            }
        }
        return StepResult.CONTINUE;
    }

    /**
     * @return 玩家是否有足够的材料
     */
    private boolean hasMaterial() {
        MutableInt pistonCount = new MutableInt(0);
        MutableInt levelCount = new MutableInt(0);
        // 遍历物品栏和副手，不遍历盔甲槽
        ArrayList<ItemStack> list = new ArrayList<>(this.getFakePlayer().getInventory().getNonEquipmentItems());
        list.add(this.getFakePlayer().getOffhandItem());
        if (hasMaterial(list, pistonCount, levelCount)) {
            return true;
        }
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value()) {
            return list.stream()
                    .filter(InventoryUtils::isOperableSulkerBox)
                    .map(ContainerComponentInventory::new)
                    .anyMatch(inventory -> hasMaterial(inventory, pistonCount, levelCount));
        }
        return false;
    }

    private static boolean hasMaterial(Iterable<ItemStack> list, MutableInt pistonCount, MutableInt levelCount) {
        for (ItemStack itemStack : list) {
            if (itemStack.is(Items.PISTON)) {
                pistonCount.add(itemStack.getCount());
            } else if (itemStack.is(Items.LEVER)) {
                levelCount.add(itemStack.getCount());
            }
            if (pistonCount.intValue() >= 2 && levelCount.intValue() >= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在基岩上方放置一个朝上的活塞
     *
     * @return 是否放置成功
     */
    private StepResult placePiston(BlockPos bedrockPos) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        Level world = ServerUtils.getWorld(fakePlayer);
        if (this.aboveHasFallingBlockEntity(bedrockPos)) {
            return StepResult.COMPLETION;
        }
        BlockPos up = bedrockPos.above(1);
        BlockState blockState = world.getBlockState(up);
        boolean isPiston = false;
        //noinspection StatementWithEmptyBody
        if (isReplaceableBlock(blockState)) {
            // 当前方块是可以被直接替换的，例如雪
            // 什么也不需要不做
        } else if (blockState.is(Blocks.PISTON) && blockState.getValue(PistonBaseBlock.FACING) == Direction.UP) {
            // 当方块已经是活塞了，不需要再次放置
            isPiston = true;
        } else if (canMine(blockState, world, up)) {
            return tickBreakBlock(up);
        } else {
            return StepResult.COMPLETION;
        }
        up = bedrockPos.above(2);
        blockState = world.getBlockState(up);
        if (isPiston && blockState.is(Blocks.PISTON_HEAD) && blockState.getValue(PistonHeadBlock.FACING) == Direction.UP) {
            // 上方方块是下方活塞伸出的活塞头
            return StepResult.CONTINUE;
        }
        // 活塞上方的方块不会影响活塞退出
        if (blockState.isAir() || blockState.getPistonPushReaction() == PushReaction.DESTROY) {
            if (isPiston) {
                return StepResult.CONTINUE;
            }
            // 放置活塞
            if (placePiston(bedrockPos, Direction.UP).consumesAction()) {
                return StepResult.CONTINUE;
            }
            return StepResult.COMPLETION;
        } else if (canMine(blockState, world, up)) {
            return tickBreakBlock(up);
        } else {
            return StepResult.COMPLETION;
        }
    }

    private boolean canMine(BlockState blockState, Level world, BlockPos blockPos) {
        if (blockState.isAir()) {
            return true;
        }
        boolean isPiston = blockState.is(Blocks.PISTON);
        if (isPiston) {
            // 允许破坏方向不正确的活塞
            if (blockState.getValue(PistonBaseBlock.FACING).getAxis() != Direction.Axis.Y) {
                return true;
            }
            // 允许破坏浮空的活塞
            if (!world.getBlockState(blockPos.below()).is(Blocks.BEDROCK)) {
                return true;
            }
            // 允许破坏放置在基岩上但朝下的活塞
            if (blockState.getValue(PistonBaseBlock.FACING) == Direction.DOWN) {
                return true;
            }
        }
        // 允许破坏没有附着在方块侧面的拉杆
        if (blockState.is(Blocks.LEVER)) {
            return blockState.getValue(LeverBlock.FACE) != AttachFace.WALL;
        }
        if (isPiston || blockState.is(Blocks.PISTON_HEAD)) {
            return false;
        }
        return blockState.getDestroySpeed(world, blockPos) != -1 && this.excavator.canBreak(blockPos);
    }

    /**
     * 在基岩上放置并激活拉杆
     *
     * @return 拉杆是否放置并激活成功
     */
    private StepResult placeAndActivateTheLever(BedrockBreakingContext context) {
        BlockPos bedrockPos = context.getBedrockPos();
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        Level world = ServerUtils.getWorld(fakePlayer);
        ServerPlayerGameMode interactionManager = fakePlayer.gameMode;
        Direction direction = null;
        for (Direction value : MathUtils.HORIZONTAL) {
            BlockPos offset = bedrockPos.relative(value);
            BlockState blockState = world.getBlockState(offset);
            if (isReplaceableBlock(blockState)) {
                direction = value;
                continue;
            }
            if (blockState.is(Blocks.LEVER)) {
                // 拉杆没有附着在墙壁上，破坏拉杆
                if (blockState.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) != AttachFace.WALL) {
                    return tickBreakBlock(offset);
                }
                if (bedrockPos.equals(offset.relative(blockState.getValue(LeverBlock.FACING), -1))) {
                    if (context.getLeverPos() == null) {
                        context.setLeverPos(offset);
                        if (blockState.getValue(LeverBlock.POWERED)) {
                            continue;
                        }
                        // 激活拉杆
                        interactionLever(offset);
                    } else {
                        // 拉杆正确的附着在了基岩上，但是拉杆不止一个
                        return tickBreakBlock(offset);
                    }
                } else {
                    BlockPos supportBlockPos = offset.relative(blockState.getValue(LeverBlock.FACING), -1);
                    // 拉杆附着在了另一个基岩上
                    if (world.getBlockState(supportBlockPos).is(Blocks.BEDROCK) && this.inSelectionArea(supportBlockPos)) {
                        continue;
                    }
                    // 拉杆附着在了墙上，但不是当前要破坏的基岩方块
                    return tickBreakBlock(offset);
                }
            } else if (this.inSelectionArea(offset) && canMine(blockState, world, offset)) {
                return tickBreakBlock(offset);
            }
        }
        if (context.getLeverPos() != null) {
            return StepResult.CONTINUE;
        }
        if (direction == null) {
            return StepResult.COMPLETION;
        }
        // 没有正确的拉杆附着在基岩上，放置并激活拉杆
        BlockPos offset = bedrockPos.relative(direction);
        BlockState blockState = world.getBlockState(offset.below());
        if (blockState.is(Blocks.PISTON) && blockState.getValue(PistonBaseBlock.FACING) == Direction.UP) {
            // 当前位置下方是未伸出的活塞，不能在这里放置拉杆
            return StepResult.COMPLETION;
        }
        if (blockState.is(Blocks.MOVING_PISTON)) {
            // 当前位置下方是移动的活塞
            return StepResult.COMPLETION;
        }
        this.inventory.replenishment(InteractionHand.OFF_HAND, stack -> stack.is(Items.LEVER));
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        BlockHitResult hitResult = new BlockHitResult(bedrockPos.getCenter(), direction, bedrockPos, false);
        // 放置拉杆
        interactionManager.useItemOn(fakePlayer, world, fakePlayer.getOffhandItem(), InteractionHand.OFF_HAND, hitResult);
        // 再次单击激活拉杆
        interactionLever(offset);
        context.setLeverPos(offset);
        return StepResult.CONTINUE;
    }

    /**
     * 判断当前方块是否是可替换的，只对雪片进行了特判，其他带有{@code #minecraft:replaceable}标签的方块，例如藤蔓，发光地衣等会直接返回{@code true}
     *
     * @return 当前方块是否是可直接替换的
     */
    private boolean isReplaceableBlock(BlockState blockState) {
        if (blockState.isAir()) {
            return true;
        }
        if (blockState.is(BlockTags.REPLACEABLE)) {
            if (blockState.is(Blocks.SNOW)) {
                // 只有层数为1的雪片才能被替换
                return blockState.getValue(SnowLayerBlock.LAYERS) == 1;
            }
            return true;
        }
        return false;
    }

    /**
     * 破除基岩
     */
    private StepResult pistonBreakBedrock(BedrockBreakingContext context) {
        BlockPos bedrockPos = context.getBedrockPos();
        BlockPos up = bedrockPos.above();
        // 基岩上方方块是活塞
        Level world = ServerUtils.getWorld(this.getFakePlayer());
        BlockState blockState = world.getBlockState(up);
        if (blockState.is(Blocks.PISTON) && blockState.getValue(PistonBaseBlock.EXTENDED)) {
            // 先切换工具，再计算剩余挖掘时间
            switchTool(blockState, world, up, this.getFakePlayer());
            // 计算剩余挖掘时间
            int currentTime = this.excavator.computingRemainingMiningTime(up);
            if (currentTime == 1) {
                // 方块将在本游戏刻挖掘完毕
                BlockPos leverPos = context.getLeverPos();
                BlockState leverState = world.getBlockState(leverPos);
                if (leverState.is(Blocks.LEVER)) {
                    if (leverState.getValue(LeverBlock.POWERED)) {
                        // 关闭附着在基岩上的拉杆
                        interactionLever(leverPos);
                    }
                    // 关闭周围可能激活活塞的拉杆
                    closeTheSurroundingLevers(up);
                    context.setLeverPos(null);
                    // 继续挖掘，此时活塞应该会挖掘完毕
                    breakBlock(up, false);
                    // 放置一个朝下的活塞，这个活塞会破坏掉基岩
                    if (placePiston(bedrockPos, Direction.DOWN).consumesAction()) {
                        return StepResult.COMPLETION;
                    }
                }
                return StepResult.COMPLETION;
            }
            breakBlock(up, false);
            return StepResult.TICK_COMPLETION;
        } else {
            return StepResult.FAIL;
        }
    }

    /**
     * 关闭周围所有可能激活活塞的拉杆
     */
    private void closeTheSurroundingLevers(BlockPos pistonPos) {
        Level world = ServerUtils.getWorld(this.getFakePlayer());
        Consumer<BlockPos> consumer = blockPos -> {
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.isAir()) {
                return;
            }
            if (blockState.is(Blocks.LEVER) && blockState.getValue(LeverBlock.POWERED)) {
                interactionLever(blockPos);
            }
        };
        for (Direction direction : MathUtils.HORIZONTAL) {
            BlockPos offset = pistonPos.relative(direction);
            // 活塞周围的拉杆
            consumer.accept(offset);
            // 活塞周围的方块，这些方块可能是拉杆的支撑方块
            for (Direction value : MathUtils.HORIZONTAL) {
                consumer.accept(offset.relative(value));
            }
        }
        for (Direction direction : MathUtils.HORIZONTAL) {
            BlockPos offset = pistonPos.above().relative(direction);
            consumer.accept(offset);
            for (Direction value : MathUtils.HORIZONTAL) {
                consumer.accept(offset.relative(value));
            }
        }
        consumer.accept(pistonPos.above(2));
        for (Direction direction : MathUtils.HORIZONTAL) {
            consumer.accept(pistonPos.above(2).relative(direction));
        }
        // 活塞上方第三格可能放着朝下的拉杆
        BlockPos up = pistonPos.above(3);
        BlockState upBlockState = world.getBlockState(up);
        if (upBlockState.is(Blocks.LEVER) && upBlockState.getValue(LeverBlock.POWERED) && upBlockState.getValue(LeverBlock.FACE) == AttachFace.FLOOR) {
            interactionLever(up);
        }
        // 活塞下方第二格可能放着朝上的拉杆
        BlockPos down = pistonPos.below(2);
        BlockState downBlockState = world.getBlockState(down);
        if (downBlockState.is(Blocks.LEVER) && downBlockState.getValue(LeverBlock.POWERED) && downBlockState.getValue(LeverBlock.FACE) == AttachFace.CEILING) {
            interactionLever(down);
        }
    }

    /**
     * 挖掘掉破完基岩后留下的活塞
     */
    private boolean cleanPiston(BlockPos blockPos) {
        BlockState blockState = ServerUtils.getWorld(this.getFakePlayer()).getBlockState(blockPos);
        if (blockState.isAir()) {
            return true;
        }
        // 移动的活塞
        if (blockState.is(Blocks.MOVING_PISTON)) {
            return false;
        }
        if (blockState.is(Blocks.PISTON)) {
            return breakBlock(blockPos, true);
        }
        // 如果返回false，可能引发更多问题
        return true;
    }

    private StepResult tickBreakBlock(BlockPos blockPos) {
        EntityPlayerMPFake player = this.excavator.getPlayer();
        Level world = ServerUtils.getWorld(player);
        if (FallingBlock.isFree(world.getBlockState(blockPos.above()))) {
            // 等待上方下落的方块实体落下
            if (aboveHasFallingBlockEntity(blockPos)) {
                return StepResult.COMPLETION;
            }
        }
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.is(Blocks.TORCH) || blockState.is(Blocks.WALL_TORCH)) {
            // 挖掘火把上方的重力方块，只检查上方一格可能发生误判
            for (int i = 1; i <= 2; i++) {
                BlockPos up = blockPos.above(i);
                if (world.getBlockState(up).getBlock() instanceof FallingBlock) {
                    if (canInteract(up)) {
                        return breakBlock(up, true) ? StepResult.COMPLETION : StepResult.TICK_COMPLETION;
                    } else {
                        return StepResult.COMPLETION;
                    }
                }
            }
        }
        if (world.getBlockState(blockPos).getBlock() instanceof FallingBlock) {
            // 如果有火把，挖掘最下方的重力方块并放置火把
            // 否则，挖掘最上方的重力方块
            boolean hasTorch = this.hasTorch();
            while (true) {
                Direction direction = hasTorch ? Direction.DOWN : Direction.UP;
                BlockPos offset = blockPos.relative(direction);
                if (world.getBlockState(offset).getBlock() instanceof FallingBlock && canInteract(offset.relative(direction))) {
                    blockPos = offset;
                } else {
                    break;
                }
            }
            boolean broken = breakBlock(blockPos, true);
            if (broken && hasTorch) {
                this.inventory.replenishment(InteractionHand.OFF_HAND, itemStack -> itemStack.is(Items.TORCH));
                placeBlock(blockPos);
                return StepResult.COMPLETION;
            } else {
                return StepResult.TICK_COMPLETION;
            }
        }
        return breakBlock(blockPos, true) ? StepResult.COMPLETION : StepResult.TICK_COMPLETION;
    }

    /**
     * @return 玩家是否有火把
     */
    private boolean hasTorch() {
        return this.inventory.contains(itemStack -> itemStack.is(Items.TORCH));
    }

    /**
     * @return 如果玩家有火把，返回上方是否有下落的方块，否则返回{@code false}
     */
    private boolean aboveHasFallingBlockEntity(BlockPos blockPos) {
        return this.hasTorch() && this.hasFallingSand(0, blockPos);
    }

    /**
     * 玩家上方是否有下落的方块
     */
    @SuppressWarnings("SameParameterValue")
    private boolean hasFallingSand(int range) {
        return this.hasFallingSand(range, this.getFakePlayer().blockPosition());
    }

    private boolean hasFallingSand(int range, BlockPos blockPos) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        Level world = ServerUtils.getWorld(fakePlayer);
        BlockPos from = new BlockPos(blockPos.getX() - range, blockPos.getY(), blockPos.getZ() - range);
        BlockPos to = new BlockPos(blockPos.getX() + range, ServerUtils.getMaxArchitectureAltitude(world), blockPos.getZ() + range);
        return new EntityTraverser<>(world, from, to, FallingBlockEntity.class).isPresent();
    }

    /**
     * 尝试破坏指定位置的方块
     *
     * @param switchTool 是否需要切换工具
     * @return 是否破坏成功
     */
    private boolean breakBlock(BlockPos blockPos, boolean switchTool) {
        this.hasAction = true;
        EntityPlayerMPFake player = this.excavator.getPlayer();
        Level world = ServerUtils.getWorld(player);
        BlockState blockState = world.getBlockState(blockPos);
        if (switchTool) {
            switchTool(blockState, world, blockPos, player);
        }
        return this.excavator.mining(blockPos, Direction.DOWN, false);
    }

    private void switchTool(BlockState blockState, Level world, BlockPos blockPos, EntityPlayerMPFake player) {
        boolean replenishment = this.inventory.replenishment(itemStack -> {
            if (this.getFakePlayer().isCreative()) {
                return itemStack.getItem().canDestroyBlock(player.getMainHandItem(), blockState, world, blockPos, player);
            }
            if (itemStack.isEmpty()) {
                return false;
            }
            // 不使用低耐久工具
            if (isDamaged(itemStack)) {
                return false;
            }
            return itemStack.getDestroySpeed(blockState) > 1F;
        });
        if (replenishment) {
            return;
        }
        // 工具没有切换成功，使用其他物品替换手上工具以避免工具损坏
        this.inventory.replenishment(itemStack -> !isDamaged(itemStack));
    }

    /**
     * @return 指定物品是否是低耐久且有经验修补的物品
     */
    private boolean isDamaged(ItemStack itemStack) {
        return itemStack.isDamageableItem() && itemStack.getMaxDamage() - itemStack.getDamageValue() <= 10 && EnchantmentUtils.canRepairWithXp(itemStack);
    }

    /**
     * 放置活塞
     */
    private InteractionResult placePiston(BlockPos bedrockPos, Direction direction) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerPlayerGameMode interactionManager = fakePlayer.gameMode;
        // 看向与活塞相反的方向
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        this.inventory.replenishment(InteractionHand.OFF_HAND, itemStack -> itemStack.is(Items.PISTON));
        // 放置活塞
        BlockHitResult hitResult = new BlockHitResult(Vec3.upFromBottomCenterOf(bedrockPos, 1.0), direction, bedrockPos.above(), false);
        InteractionResult result = interactionManager.useItemOn(fakePlayer, ServerUtils.getWorld(fakePlayer), fakePlayer.getOffhandItem(), InteractionHand.OFF_HAND, hitResult);
        if (result.consumesAction()) {
            this.hasAction = true;
        }
        return result;
    }

    /**
     * 放置一个方块
     */
    private void placeBlock(BlockPos blockPos) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerPlayerGameMode interactionManager = fakePlayer.gameMode;
        fakePlayer.lookAt(EntityAnchorArgument.Anchor.EYES, blockPos.getCenter());
        BlockHitResult hitResult = new BlockHitResult(Vec3.upFromBottomCenterOf(blockPos, 1.0), Direction.DOWN, blockPos, false);
        interactionManager.useItemOn(fakePlayer, ServerUtils.getWorld(fakePlayer), fakePlayer.getOffhandItem(), InteractionHand.OFF_HAND, hitResult);
    }

    /**
     * 单击一次拉杆
     */
    private void interactionLever(BlockPos leverPos) {
        ServerPlayerGameMode interactionManager = this.getFakePlayer().gameMode;
        BlockHitResult hitResult = new BlockHitResult(leverPos.getCenter(), Direction.UP, leverPos, false);
        InteractionResult result = interactionManager.useItemOn(this.getFakePlayer(), ServerUtils.getWorld(this.getFakePlayer()), this.getFakePlayer().getMainHandItem(), InteractionHand.MAIN_HAND, hitResult);
        if (result.consumesAction()) {
            this.hasAction = true;
        }
    }

    /**
     * @return 是否需要进食
     */
    private boolean shouldEat() {
        if (this.inventory.contains(InventoryUtils::isFoodItem)) {
            if (this.getFakePlayer().getAbilities().invulnerable) {
                return false;
            }
            FoodData hungerManager = this.getFakePlayer().getFoodData();
            if (hungerManager.getFoodLevel() <= 10) {
                return true;
            }
            if (hungerManager.needsFood()) {
                return this.getFakePlayer().getMaxHealth() - this.getFakePlayer().getHealth() > 2;
            }
        }
        return false;
    }

    /**
     * 排除熔岩
     */
    private void drainFluidLava() {
        Iterator<BlockPos> iterator = this.lavas.iterator();
        while (iterator.hasNext()) {
            BlockPos blockPos = iterator.next();
            EntityPlayerMPFake fakePlayer = this.getFakePlayer();
            Level world = ServerUtils.getWorld(fakePlayer);
            if (this.canInteract(blockPos) &&
                (this.inventory.replenishment(InteractionHand.OFF_HAND, canDrainFluid(world, blockPos)) ||
                 this.inventory.replenishment(InteractionHand.OFF_HAND, itemStack -> itemStack.is(Items.PISTON)))) {
                placeBlock(blockPos);
            }
            iterator.remove();
        }
    }

    private Predicate<ItemStack> canDrainFluid(Level world, BlockPos blockPos) {
        return itemStack -> {
            if (itemStack.is(Items.PISTON) || itemStack.is(Items.REDSTONE_BLOCK)) {
                return false;
            }
            if (itemStack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                // 避免放置潜影盒
                if (block instanceof BaseEntityBlock) {
                    return false;
                }
                return block.defaultBlockState().isRedstoneConductor(world, blockPos);
            }
            return false;
        };
    }

    /**
     * 吃食物
     */
    private void eat() {
        PlayerWorkPhase prev = this.prevPhase == PlayerWorkPhase.EAT ? PlayerWorkPhase.WORK : this.prevPhase;
        if (this.getFakePlayer().getAbilities().invulnerable) {
            this.setPhase(prev);
            return;
        }
        if (this.getFakePlayer().canEat(false)) {
            if (this.getFakePlayer().getUseItem().isEmpty()) {
                if (this.inventory.replenishment(InventoryUtils::isFoodItem)) {
                    ServerPlayerGameMode interactionManager = this.getFakePlayer().gameMode;
                    Level world = ServerUtils.getWorld(this.getFakePlayer());
                    ItemStack food = this.getFakePlayer().getMainHandItem();
                    interactionManager.useItem(this.getFakePlayer(), world, food, InteractionHand.MAIN_HAND);
                } else {
                    this.setPhase(prev);
                }
            }
        } else {
            this.setPhase(prev);
        }
    }

    /**
     * 收集材料
     */
    private void collectingMaterials() {
        boolean finished = this.pathfinder.isFinished();
        if (finished) {
            // 玩家可能在到达目标位置的前一瞬间捡起物品，导致在路径在走完之前被更新并不会执行到这里，但这不是问题
            dropGarbageAndCollectMaterial();
        }
        // 目标掉落物的位置不可到达
        if ((this.pathfinder.isInvalid() || this.pathfinder.isInaccessible()) && !this.itemEntities.isEmpty()) {
            this.itemEntities.remove(this.recentItemEntity);
            this.pathfinder.pause(1);
            this.materialCollectionComplete = true;
            this.recentItemEntity = null;
            // 要回收的最后一个物品不可到达，结束材料回收
            if (this.itemEntities.isEmpty()) {
                this.setPhase(PlayerWorkPhase.WORK);
                return;
            }
        }
        if (this.itemEntities.isEmpty()) {
            AABB box = this.traverser.toBox().inflate(10.0);
            List<ItemEntity> list = ServerUtils.getWorld(this.getFakePlayer()).getEntitiesOfClass(ItemEntity.class, box)
                    .stream()
                    .filter(itemEntity -> isMaterial(itemEntity.getItem()))
                    .toList();
            this.itemEntities.addAll(list);
        }
        // 没有材料可以回收
        if (this.itemEntities.isEmpty()) {
            this.setPhase(PlayerWorkPhase.WORK);
            return;
        }
        if (this.recentItemEntity != null) {
            return;
        }
        ItemEntity recentEntity = this.itemEntities.getFirst();
        for (ItemEntity itemEntity : this.itemEntities) {
            if (recentEntity.distanceTo(getFakePlayer()) > itemEntity.distanceTo(getFakePlayer())) {
                recentEntity = itemEntity;
            }
        }
        this.recentItemEntity = recentEntity;
    }

    /**
     * 丢弃垃圾物品，然后把身上最多的材料放入潜影盒
     */
    private void dropGarbageAndCollectMaterial() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        Inventory inventory = fakePlayer.getInventory();
        Level world = ServerUtils.getWorld(fakePlayer);
        BlockPos blockPos = fakePlayer.blockPosition();
        ItemStack mostStack = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
            ItemStack itemStack = inventory.getNonEquipmentItems().get(i);
            if (itemStack.isEmpty()) {
                // 玩家物品栏里还有空槽位
                return;
            }
            if (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().isRedstoneConductor(world, blockPos)) {
                if (itemStack.getCount() > mostStack.getCount()) {
                    mostStack = itemStack;
                }
            }
        }
        final ItemStack finalMostStack = mostStack;
        boolean dropped = this.inventory.drop(itemStack -> itemStack != finalMostStack && isGarbage(itemStack));
        if (dropped) {
            return;
        }
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value()) {
            this.collectMaterialToShulkerBox();
            this.collectToolToShulkerBox();
        }
    }

    /**
     * 把多余的材料装入潜影盒
     */
    private void collectMaterialToShulkerBox() {
        // 获取物品栏中数量最多的物品
        ItemStack most = InventoryUtils.findMostAbundantStack(this.inventory, this::isMaterial);
        // 丢弃一组最多的物品，预留一个空槽位，后面向堆叠的空潜影盒中放入物品时会用到
        // 执行到这里时，物品栏一定是满的
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            if (InventoryUtils.canMerge(most, this.inventory.getItem(i))) {
                this.inventory.drop(i);
                break;
            }
        }
        // 整理物品栏
        this.inventory.sort();
        // 是否已经遍历到了数量最多的物品
        boolean isFoundMost = false;
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemStack = this.inventory.getItem(i);
            // 因为已经整理过，所以遍历到了空物品或潜影盒，后面所有的物品都不是材料
            if (itemStack.isEmpty() || InventoryUtils.isShulkerBoxItem(itemStack)) {
                return;
            }
            if (InventoryUtils.canMerge(itemStack, most)) {
                isFoundMost = true;
                if (this.inventory.insertWithShulkerBoxPriority(itemStack)) {
                    this.setPhase(PlayerWorkPhase.WORK);
                    return;
                }
            } else if (isFoundMost) {
                // 相同的材料是连续放置的，所以后面的物品都不是要装入潜影盒的材料
                return;
            }
        }
    }

    /**
     * 把损坏的工具放入潜影盒
     */
    private void collectToolToShulkerBox() {
        // 整理物品栏
        this.inventory.sort();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (itemStack.isEmpty() || InventoryUtils.isShulkerBoxItem(itemStack)) {
                return;
            }
            // 将已损坏的物品放入潜影盒
            if (InventoryUtils.isToolItem(itemStack) && isDamaged(itemStack) && this.inventory.insertWithShulkerBoxPriority(itemStack)) {
                return;
            }
        }
    }

    private boolean isGarbage(ItemStack itemStack) {
        if (isMaterial(itemStack)) {
            return false;
        }
        if (itemStack.is(Items.TORCH)) {
            return false;
        }
        if (InventoryUtils.isFoodItem(itemStack)) {
            return false;
        }
        if (InventoryUtils.isShulkerBoxItem(itemStack)) {
            return false;
        }
        return !InventoryUtils.isToolItem(itemStack);
    }

    private boolean isMaterial(ItemStack itemStack) {
        return itemStack.is(Items.PISTON) || itemStack.is(Items.LEVER);
    }

    public void add(BedrockBreakingContext context) {
        this.contexts.add(context);
    }

    public void removeIf(Predicate<BedrockBreakingContext> predicate) {
        this.contexts.removeIf(predicate);
    }

    public boolean inSelectionArea(BlockPos blockPos) {
        return this.traverser.contains(blockPos);
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(getFakePlayer().getDisplayName()));
        switch (this.regionType) {
            case CUBOID -> {
                Component from = TextProvider.blockPos(this.traverser.getMinBlockPos(), ChatFormatting.GREEN);
                Component to = TextProvider.blockPos(this.traverser.getMaxBlockPos(), ChatFormatting.GREEN);
                list.add(key.then("cuboid").then("range").translate(from, to));
            }
            case CYLINDER -> {
                CylinderBlockPosTraverser iterator = (CylinderBlockPosTraverser) this.traverser;
                Component center = TextProvider.blockPos(iterator.getCenter());
                LocalizationKey cylinder = key.then("cylinder");
                list.add(cylinder.then("center").translate(center));
                list.add(cylinder.then("radius").translate(iterator.getRadius()));
                list.add(cylinder.then("height").translate(iterator.getHeight()));
            }
        }
        if (this.ai) {
            list.add(key.then("ai").then("enable").translate());
        }
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("region_type", this.regionType.name().toLowerCase());
        switch (this.regionType) {
            case CUBOID -> {
                BlockPos minBlockPos = this.traverser.getMinBlockPos();
                json.add("from", ActionSerializeType.toJson(minBlockPos));
                BlockPos maxBlockPos = this.traverser.getMaxBlockPos();
                json.add("to", ActionSerializeType.toJson(maxBlockPos));
            }
            case CYLINDER -> {
                CylinderBlockPosTraverser iterator = (CylinderBlockPosTraverser) this.traverser;
                json.add("center", ActionSerializeType.toJson(iterator.getCenter()));
                json.addProperty("radius", iterator.getRadius());
                json.addProperty("height", iterator.getHeight());
            }
        }
        json.addProperty("ai", this.ai);
        json.addProperty("timed_material_recycling", this.recycleTimer != -1);
        return json;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.BEDROCK;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    protected void onAssignPlayer() {
        this.pathfinder = FakePlayerPathfinder.of(this::getFakePlayer, this::getMovingTarget);
        this.inventory = new PlayerStorageInventory(this.getFakePlayer());
        this.excavator = PlayerComponentCoordinator.getCoordinator(this.getFakePlayer()).getBlockExcavator();
    }

    @Override
    protected void onClearPlayer() {
        this.pathfinder = FakePlayerPathfinder.EMPTY;
        this.inventory = null;
        this.excavator = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BedrockAction that = (BedrockAction) o;
        return Objects.equals(traverser, that.traverser) && ai == that.ai;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ai, traverser);
    }

    /**
     * 获取移动目标
     */
    public Optional<BlockPos> getMovingTarget() {
        switch (this.getPhase()) {
            case WORK -> {
                Level world = ServerUtils.getWorld(this.getFakePlayer());
                if (this.bedrockTarget == null) {
                    return Optional.empty();
                }
                BlockState blockState = world.getBlockState(this.bedrockTarget);
                if (blockState.is(Blocks.BEDROCK) || this.isMovingToNearbyBedrock) {
                    return Optional.of(this.bedrockTarget);
                }
                return Optional.empty();
            }
            case COLLECT -> {
                if (this.recentItemEntity == null || this.recentItemEntity.isRemoved()) {
                    return Optional.empty();
                }
                return Optional.of(this.recentItemEntity.blockPosition());
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onStop() {
        this.pathfinder.onStop();
    }

    /**
     * 玩家当前任务的阶段
     */
    @NonNull
    private PlayerWorkPhase getPhase() {
        return phase;
    }

    private void setPhase(@NonNull PlayerWorkPhase phase) {
        this.phase = phase;
        if (this.recycleTimer != -1 && phase == PlayerWorkPhase.WORK) {
            this.recycleTimer = MATERIAL_RECYCLING_TIME;
        }
    }

    public static final class BedrockBreakingContext {
        private final BlockPos bedrockPos;
        private BlockPos leverPos;
        private BreakingState breakingState = BreakingState.PLACE_THE_PISTON_FACING_UP;

        private BedrockBreakingContext(BlockPos bedrockPos) {
            this.bedrockPos = bedrockPos;
        }

        public BlockPos getBedrockPos() {
            return this.bedrockPos;
        }

        public BlockPos getLeverPos() {
            return this.leverPos;
        }

        public void setLeverPos(BlockPos leverPos) {
            this.leverPos = leverPos;
        }

        public BreakingState getState() {
            return this.breakingState;
        }

        public void nextStep() {
            BreakingState[] values = BreakingState.values();
            if (this.breakingState.ordinal() == values.length) {
                throw new IllegalStateException();
            }
            this.breakingState = values[this.breakingState.ordinal() + 1];
        }

        public void fail() {
            this.breakingState = BreakingState.COMPLETE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BedrockBreakingContext that = (BedrockBreakingContext) o;
            return Objects.equals(bedrockPos, that.bedrockPos);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(bedrockPos);
        }
    }

    public enum BedrockRegionType {
        /**
         * 立方体范围
         */
        CUBOID,
        /**
         * 圆柱体范围
         */
        CYLINDER
    }

    public enum BreakingState {
        /**
         * 放置朝上的活塞
         */
        PLACE_THE_PISTON_FACING_UP,
        /**
         * 在基岩方块侧面放置并激活一个拉杆
         */
        PLACE_AND_ACTIVATE_THE_LEVER,
        /**
         * 挖掘基岩上方的活塞，并在挖掘完成前关闭拉杆，然后完成挖掘，接着放置一个朝下的活塞
         */
        PISTON_BREAK_BEDROCK,
        /**
         * 清理掉基岩上方的活塞
         */
        CLEAN_PISTON,
        /**
         * 已完成破基岩
         */
        COMPLETE
    }

    public enum PlayerWorkPhase {
        /**
         * 进食
         */
        EAT,
        /**
         * 破基岩
         */
        WORK,
        /**
         * 收集材料
         */
        COLLECT
    }

    /**
     * 当前步骤的执行结果
     */
    public enum StepResult {
        /**
         * 当前步骤执行完毕，应继续执行下一步
         */
        CONTINUE,
        /**
         * 不再执行下一步，但是应继续执行下一个位置
         */
        COMPLETION,
        /**
         * 不再执行下一步，并且应该结束当前tick
         */
        TICK_COMPLETION,
        /**
         * 破基岩失败，重新开始
         */
        FAIL
    }
}
