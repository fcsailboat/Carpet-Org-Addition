package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.PeriodicTaskUtils;
import org.carpetorgaddition.periodic.fakeplayer.actioncontext.BreakBedrockContext;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

public class FakePlayerBreakBedrock {
    public static void breakBedrock(BreakBedrockContext context, EntityPlayerMPFake fakePlayer) {
        World world = fakePlayer.getWorld();
        context.removeIf(destructor -> {
            if (destructor.getState() == State.COMPLETE) {
                return true;
            }
            if (world.getBlockState(destructor.getBedrockPos()).isOf(Blocks.BEDROCK)) {
                return !fakePlayer.canInteractWithBlockAt(destructor.getBedrockPos(), 0.0);
            }
            return destructor.getState() != State.CLEAN_PISTON;
        });
        double range = fakePlayer.getBlockInteractionRange();
        Box box = new Box(fakePlayer.getBlockPos()).expand(Math.min(range, 10.0));
        SelectionArea area = new SelectionArea(box);
        for (BlockPos blockPos : area) {
            if (world.getBlockState(blockPos).isOf(Blocks.BEDROCK)
                    && fakePlayer.canInteractWithBlockAt(blockPos, 0.0)
                    && context.contains(blockPos)) {
                context.add(new BedrockDestructor(blockPos));
            }
        }
        for (BedrockDestructor destructor : context) {
            int loopCount = 0;
            loop:
            while (true) {
                loopCount++;
                if (loopCount > 10) {
                    throw new InfiniteLoopException();
                }
                StepResult stepResult = start(destructor, fakePlayer, context);
                switch (stepResult) {
                    case COMPLETION -> {
                        break loop;
                    }
                    case TICK_COMPLETION -> {
                        return;
                    }
                    default -> {
                    }
                }
            }
        }
    }

    private static StepResult start(BedrockDestructor destructor, EntityPlayerMPFake fakePlayer, BreakBedrockContext context) {
        BlockPos bedrockPos = destructor.getBedrockPos();
        switch (destructor.getState()) {
            case PLACE_THE_PISTON_FACING_UP -> {
                if (hasMaterial(fakePlayer)) {
                    StepResult stepResult = placePiston(fakePlayer, bedrockPos);
                    if (stepResult == StepResult.CONTINUE) {
                        destructor.nextStep();
                    } else {
                        return stepResult;
                    }
                } else {
                    // 玩家没有足够的材料
                    return StepResult.TICK_COMPLETION;
                }
            }
            case PLACE_AND_ACTIVATE_THE_LEVER -> {
                StepResult stepResult = placeAndActivateTheLever(destructor, fakePlayer, context);
                if (stepResult == StepResult.CONTINUE) {
                    destructor.nextStep();
                } else {
                    return stepResult;
                }
                // 不管是否成功放置都结束方法
                return StepResult.COMPLETION;
            }
            case PISTON_BREAK_BEDROCK -> {
                StepResult stepResult = pistonBreakBedrock(destructor, fakePlayer);
                switch (stepResult) {
                    // 基岩破除，结束当前位置
                    case COMPLETION -> {
                        destructor.nextStep();
                        return StepResult.COMPLETION;
                    }
                    // 活塞没有挖掘完毕，结束当前tick
                    case TICK_COMPLETION -> {
                        return StepResult.TICK_COMPLETION;
                    }
                    case FAIL -> {
                        destructor.fail();
                        return StepResult.COMPLETION;
                    }
                    default -> throw new IllegalStateException();
                }
            }
            case CLEAN_PISTON -> {
                if (cleanPiston(fakePlayer, bedrockPos.up())) {
                    // 活塞挖掘完毕，执行下一步
                    destructor.nextStep();
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
    private static boolean hasMaterial(EntityPlayerMPFake fakePlayer) {
        int pistonCount = 0;
        int levelCount = 0;
        // 遍历物品栏和副手，不遍历盔甲槽
        ArrayList<ItemStack> list = new ArrayList<>(fakePlayer.getInventory().main);
        list.addAll(fakePlayer.getInventory().offHand);
        for (ItemStack itemStack : list) {
            if (itemStack.isOf(Items.PISTON)) {
                pistonCount += itemStack.getCount();
            } else if (itemStack.isOf(Items.LEVER)) {
                levelCount += itemStack.getCount();
            }
            if (pistonCount >= 2 && levelCount >= 1) {
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
    private static StepResult placePiston(EntityPlayerMPFake fakePlayer, BlockPos bedrockPos) {
        // TODO 放置时检查是否与实体相交
        World world = fakePlayer.getWorld();
        BlockPos up = bedrockPos.up(1);
        BlockState blockState = world.getBlockState(up);
        BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
        boolean isPiston = false;
        //noinspection StatementWithEmptyBody
        if (blockState.isAir() || blockState.isIn(BlockTags.REPLACEABLE)) {
            // 当前方块是可以被直接替换的，例如雪
            // 什么也不需要不做
        } else if (blockState.isOf(Blocks.PISTON)) {
            // 当方块已经是活塞了，不需要再次放置
            isPiston = true;
        } else if (canMine(fakePlayer, blockState, world, up)) {
            return tickBreakBlock(breakManager, up);
        } else {
            return StepResult.COMPLETION;
        }
        up = bedrockPos.up(2);
        blockState = world.getBlockState(up);
        if (isPiston && blockState.isOf(Blocks.PISTON_HEAD) && blockState.get(PistonHeadBlock.FACING) == Direction.UP) {
            // 上方方块是下方活塞伸出的活塞头
            return StepResult.CONTINUE;
        }
        // 活塞上方的方块不会影响活塞退出
        if (blockState.isAir() || blockState.getPistonBehavior() == PistonBehavior.DESTROY) {
            if (isPiston) {
                return StepResult.CONTINUE;
            }
            // 放置活塞
            placePiston(fakePlayer, bedrockPos, Direction.UP);
            return StepResult.CONTINUE;
        } else if (canMine(fakePlayer, blockState, world, up)) {
            return tickBreakBlock(breakManager, up);
        } else {
            return StepResult.COMPLETION;
        }
    }

    private static boolean canMine(EntityPlayerMPFake fakePlayer, BlockState blockState, World world, BlockPos blockPos) {
        if (blockState.isAir()) {
            return true;
        }
        boolean isPiston = blockState.isOf(Blocks.PISTON);
        // 允许破坏浮空的活塞
        if (isPiston && !world.getBlockState(blockPos.down()).isOf(Blocks.BEDROCK)) {
            return true;
        }
        // TODO 允许破坏不在侧面的拉杆和方向不正确的活塞
        if (blockState.isOf(Blocks.LEVER) || isPiston || blockState.isOf(Blocks.PISTON_HEAD)) {
            return false;
        }
        return blockState.getHardness(world, blockPos) != -1 && BlockBreakManager.canBreak(fakePlayer, blockPos);
    }

    /**
     * 在基岩上放置并激活拉杆
     *
     * @return 拉杆是否放置并激活成功
     */
    private static StepResult placeAndActivateTheLever(BedrockDestructor destructor, EntityPlayerMPFake fakePlayer, BreakBedrockContext context) {
        BlockPos bedrockPos = destructor.getBedrockPos();
        World world = fakePlayer.getWorld();
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        Direction direction = null;
        for (Direction value : MathUtils.HORIZONTAL) {
            BlockPos offset = bedrockPos.offset(value);
            BlockState blockState = world.getBlockState(offset);
            // TODO 改为可替换方块
            if (blockState.isAir()) {
                direction = value;
                continue;
            }
            BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
            if (blockState.isOf(Blocks.LEVER)) {
                // 拉杆没有附着在墙壁上，破坏拉杆
                if (blockState.get(WallMountedBlock.FACE) != BlockFace.WALL) {
                    return tickBreakBlock(breakManager, offset);
                }
                if (bedrockPos.equals(offset.offset(blockState.get(LeverBlock.FACING), -1))) {
                    if (destructor.getLeverPos() == null) {
                        destructor.setLeverPos(offset);
                        if (blockState.get(LeverBlock.POWERED)) {
                            continue;
                        }
                        // 激活拉杆
                        interactionLever(fakePlayer, offset);
                    } else {
                        // 拉杆正确的附着在了基岩上，但是拉杆不止一个
                        return tickBreakBlock(breakManager, offset);
                    }
                } else {
                    BlockPos supportBlockPos = offset.offset(blockState.get(LeverBlock.FACING), -1);
                    // 拉杆附着在了另一个基岩上
                    if (world.getBlockState(supportBlockPos).isOf(Blocks.BEDROCK) && context.contains(supportBlockPos)) {
                        continue;
                    }
                    // 拉杆附着在了墙上，但不是当前要破坏的基岩方块
                    return tickBreakBlock(breakManager, offset);
                }
            } else if (canMine(fakePlayer, blockState, world, offset)) {
                return tickBreakBlock(breakManager, offset);
            }
        }
        if (destructor.getLeverPos() != null) {
            return StepResult.CONTINUE;
        }
        if (direction == null) {
            return StepResult.COMPLETION;
        }
        // 没有正确的拉杆附着在基岩上，放置并激活拉杆
        BlockPos offset = bedrockPos.offset(direction);
        BlockState blockState = world.getBlockState(offset.down());
        if (blockState.isOf(Blocks.PISTON) && blockState.get(PistonBlock.FACING) == Direction.UP) {
            // 当前位置下方是未伸出的活塞，不能在这里放置拉杆
            return StepResult.COMPLETION;
        }
        if (blockState.isOf(Blocks.MOVING_PISTON)) {
            // 当前位置下方是移动的活塞
            return StepResult.COMPLETION;
        }
        FakePlayerUtils.replenishment(fakePlayer, Hand.OFF_HAND, stack -> stack.isOf(Items.LEVER));
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        BlockHitResult hitResult = new BlockHitResult(bedrockPos.toCenterPos(), direction, bedrockPos, false);
        // 放置拉杆
        interactionManager.interactBlock(fakePlayer, world, fakePlayer.getOffHandStack(), Hand.OFF_HAND, hitResult);
        // 再次单击激活拉杆
        interactionLever(fakePlayer, offset);
        destructor.setLeverPos(offset);
        return StepResult.CONTINUE;
    }

    /**
     * 破除基岩
     */
    private static StepResult pistonBreakBedrock(BedrockDestructor destructor, EntityPlayerMPFake fakePlayer) {
        BlockPos bedrockPos = destructor.getBedrockPos();
        BlockPos up = bedrockPos.up();
        // 基岩上方方块是活塞
        World world = fakePlayer.getWorld();
        BlockState blockState = world.getBlockState(up);
        if (blockState.isOf(Blocks.PISTON) && blockState.get(PistonBlock.EXTENDED)) {
            BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
            // 先切换工具，再计算剩余挖掘时间
            switchTool(blockState, world, up, fakePlayer);
            // 计算剩余挖掘时间
            int currentTime = breakManager.getCurrentBreakingTime(up);
            if (currentTime == 1) {
                // 方块将在本游戏刻挖掘完毕
                BlockPos leverPos = destructor.getLeverPos();
                BlockState leverState = world.getBlockState(leverPos);
                if (leverState.isOf(Blocks.LEVER)) {
                    if (leverState.get(LeverBlock.POWERED)) {
                        // 关闭附着在基岩上的拉杆
                        interactionLever(fakePlayer, leverPos);
                    }
                    // 关闭周围可能激活活塞的拉杆
                    closeTheSurroundingLevers(up, fakePlayer);
                    destructor.setLeverPos(null);
                    // 继续挖掘，此时活塞应该会挖掘完毕
                    breakBlock(breakManager, up, false);
                    // 放置一个朝下的活塞，这个活塞会破坏掉基岩
                    placePiston(fakePlayer, bedrockPos, Direction.DOWN);
                    return StepResult.COMPLETION;
                }
                return StepResult.COMPLETION;
            }
            breakBlock(breakManager, up, false);
            return StepResult.TICK_COMPLETION;
        } else {
            return StepResult.FAIL;
        }
    }

    /**
     * 关闭周围所有可能激活活塞的拉杆
     */
    private static void closeTheSurroundingLevers(BlockPos pistonPos, EntityPlayerMPFake fakePlayer) {
        World world = fakePlayer.getWorld();
        Consumer<BlockPos> consumer = blockPos -> {
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.isAir()) {
                return;
            }
            if (blockState.isOf(Blocks.LEVER) && blockState.get(LeverBlock.POWERED)) {
                interactionLever(fakePlayer, blockPos);
            }
        };
        for (Direction direction : MathUtils.HORIZONTAL) {
            BlockPos offset = pistonPos.offset(direction);
            // 活塞周围的拉杆
            consumer.accept(offset);
            // 活塞周围的方块，这些方块可能是拉杆的支撑方块
            for (Direction value : MathUtils.HORIZONTAL) {
                consumer.accept(offset.offset(value));
            }
        }
        for (Direction direction : MathUtils.HORIZONTAL) {
            BlockPos offset = pistonPos.up().offset(direction);
            consumer.accept(offset);
            for (Direction value : MathUtils.HORIZONTAL) {
                consumer.accept(offset.offset(value));
            }
        }
        consumer.accept(pistonPos.up(2));
        for (Direction direction : MathUtils.HORIZONTAL) {
            consumer.accept(pistonPos.up(2).offset(direction));
        }
        // 活塞上方第三格可能放着朝下的拉杆
        BlockPos up = pistonPos.up(3);
        BlockState upBlockState = world.getBlockState(up);
        if (upBlockState.isOf(Blocks.LEVER) && upBlockState.get(LeverBlock.POWERED) && upBlockState.get(LeverBlock.FACE) == BlockFace.FLOOR) {
            interactionLever(fakePlayer, up);
        }
        // 活塞下方第二格可能放着朝上的拉杆
        BlockPos down = pistonPos.down(2);
        BlockState downBlockState = world.getBlockState(down);
        if (downBlockState.isOf(Blocks.LEVER) && downBlockState.get(LeverBlock.POWERED) && downBlockState.get(LeverBlock.FACE) == BlockFace.CEILING) {
            interactionLever(fakePlayer, down);
        }
    }

    /**
     * 挖掘掉破完基岩后留下的活塞
     */
    private static boolean cleanPiston(EntityPlayerMPFake fakePlayer, BlockPos blockPos) {
        BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
        BlockState blockState = fakePlayer.getWorld().getBlockState(blockPos);
        if (blockState.isAir()) {
            return true;
        }
        // 移动的活塞
        if (blockState.isOf(Blocks.MOVING_PISTON)) {
            return false;
        }
        if (blockState.isOf(Blocks.PISTON)) {
            return breakBlock(breakManager, blockPos, true);
        }
        return true;
    }

    private static StepResult tickBreakBlock(BlockBreakManager breakManager, BlockPos blockPos) {
        return breakBlock(breakManager, blockPos, true) ? StepResult.COMPLETION : StepResult.TICK_COMPLETION;
    }

    /**
     * 尝试破坏指定位置的方块
     *
     * @param switchTool 是否需要切换工具
     * @return 是否破坏成功
     */
    private static boolean breakBlock(BlockBreakManager breakManager, BlockPos blockPos, boolean switchTool) {
        EntityPlayerMPFake player = breakManager.getPlayer();
        World world = player.getWorld();
        BlockState blockState = world.getBlockState(blockPos);
        if (switchTool) {
            switchTool(blockState, world, blockPos, player);
        }
        return breakManager.breakBlock(blockPos, Direction.DOWN, false);
    }

    private static void switchTool(BlockState blockState, World world, BlockPos blockPos, EntityPlayerMPFake player) {
        boolean replenishment = FakePlayerUtils.replenishment(player, itemStack -> {
            if (player.isCreative()) {
                return itemStack.getItem().canMine(blockState, world, blockPos, player);
            }
            if (itemStack.isEmpty()) {
                return false;
            }
            // 不使用低耐久工具
            if (isDamaged(itemStack)) {
                return false;
            }
            return itemStack.getMiningSpeedMultiplier(blockState) > 1F;
        });
        if (replenishment) {
            return;
        }
        // 工具没有切换成功，使用其他物品替换手上工具以避免工具损坏
        FakePlayerUtils.replenishment(player, itemStack -> !isDamaged(itemStack));
    }

    /**
     * @return 指定物品是否是低耐久且有经验修补的物品
     */
    private static boolean isDamaged(ItemStack itemStack) {
        return itemStack.isDamageable() && itemStack.getMaxDamage() - itemStack.getDamage() <= 10 && EnchantmentUtils.canRepairWithXp(itemStack);
    }

    /**
     * 放置活塞
     */
    private static void placePiston(EntityPlayerMPFake fakePlayer, BlockPos bedrockPos, Direction direction) {
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        // 看向与活塞相反的方向
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        FakePlayerUtils.replenishment(fakePlayer, Hand.OFF_HAND, itemStack -> itemStack.isOf(Items.PISTON));
        // 放置活塞
        BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(bedrockPos, 1.0), direction, bedrockPos.up(), false);
        interactionManager.interactBlock(fakePlayer, fakePlayer.getWorld(), fakePlayer.getOffHandStack(), Hand.OFF_HAND, hitResult);
    }

    /**
     * 单击一次拉杆
     */
    private static void interactionLever(EntityPlayerMPFake fakePlayer, BlockPos leverPos) {
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        BlockHitResult hitResult = new BlockHitResult(leverPos.toCenterPos(), Direction.UP, leverPos, false);
        interactionManager.interactBlock(fakePlayer, fakePlayer.getWorld(), fakePlayer.getMainHandStack(), Hand.MAIN_HAND, hitResult);
    }

    public static class BedrockDestructor {
        private final BlockPos bedrockPos;
        private BlockPos leverPos;
        private State state = State.PLACE_THE_PISTON_FACING_UP;

        private BedrockDestructor(BlockPos bedrockPos) {
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

        public State getState() {
            return this.state;
        }

        public void nextStep() {
            State[] values = State.values();
            if (this.state.ordinal() == values.length) {
                throw new IllegalStateException();
            }
            this.state = values[this.state.ordinal() + 1];
        }

        public void fail() {
            this.state = State.COMPLETE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BedrockDestructor that = (BedrockDestructor) o;
            return Objects.equals(bedrockPos, that.bedrockPos);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(bedrockPos);
        }
    }

    public enum State {
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

    /**
     * 当前步骤的执行结果
     */
    private enum StepResult {
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
