package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.WallMountedBlock;
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
import org.carpetorgaddition.periodic.PeriodicTaskUtils;
import org.carpetorgaddition.periodic.fakeplayer.actioncontext.BreakBedrockContext;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.Objects;

public class FakePlayerBreakBedrock {
    public static void breakBedrock(BreakBedrockContext context, EntityPlayerMPFake fakePlayer) {
        World world = fakePlayer.getWorld();
        SelectionArea area = new SelectionArea(new Box(fakePlayer.getBlockPos()).expand(5, 1, 5));
        context.remove();
        for (BlockPos blockPos : area) {
            if (world.getBlockState(blockPos).isOf(Blocks.BEDROCK)) {
                context.add(new BedrockDestructor(blockPos));
            }
        }
        for (BedrockDestructor destructor : context) {
            loop:
            while (true) {
                StepResult stepResult = start(destructor, fakePlayer);
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

    private static StepResult start(BedrockDestructor destructor, EntityPlayerMPFake fakePlayer) {
        BlockPos bedrockPos = destructor.getBedrockPos();
        switch (destructor.getState()) {
            case PLACE_THE_PISTON_FACING_UP -> {
                if (hasMaterial(fakePlayer) && placePiston(fakePlayer, bedrockPos)) {
                    destructor.nextStep();
                } else {
                    return StepResult.COMPLETION;
                }
            }
            case PLACE_AND_ACTIVATE_THE_LEVER -> {
                if (placeAndActivateTheLever(destructor, fakePlayer)) {
                    destructor.nextStep();
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
        for (ItemStack itemStack : fakePlayer.getInventory().main) {
            if (pistonCount >= 2 && levelCount >= 1) {
                return true;
            }
            if (itemStack.isOf(Items.PISTON)) {
                pistonCount += itemStack.getCount();
                continue;
            }
            if (itemStack.isOf(Items.LEVER)) {
                levelCount += itemStack.getCount();
            }
        }
        return false;
    }

    /**
     * 在基岩上方放置一个朝上的活塞
     *
     * @return 是否放置成功
     */
    private static boolean placePiston(EntityPlayerMPFake fakePlayer, BlockPos bedrockPos) {
        World world = fakePlayer.getWorld();
        BlockState blockState = world.getBlockState(bedrockPos.up(1));
        boolean isPiston = false;
        //noinspection StatementWithEmptyBody
        if (blockState.isIn(BlockTags.REPLACEABLE)) {
            // 当前方块是可以被直接替换的，例如雪
            // 什么也不需要不做
        } else {
            if (blockState.isOf(Blocks.PISTON)) {
                // 当方块已经是活塞了，不需要再次放置
                isPiston = true;
            } else {
                return false;
            }
        }
        blockState = world.getBlockState(bedrockPos.up(2));
        // 活塞上方的方块不会影响活塞退出
        if (blockState.isAir() || blockState.getPistonBehavior() == PistonBehavior.DESTROY) {
            if (isPiston) {
                return true;
            }
            // 放置活塞
            placePiston(fakePlayer, bedrockPos, Direction.UP);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 在基岩上放置并激活拉杆
     *
     * @return 拉杆是否放置并激活成功
     */
    private static boolean placeAndActivateTheLever(BedrockDestructor destructor, EntityPlayerMPFake fakePlayer) {
        BlockPos bedrockPos = destructor.getBedrockPos();
        World world = fakePlayer.getWorld();
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        Direction direction = null;
        for (Direction value : MathUtils.HORIZONTAL) {
            BlockPos offset = bedrockPos.offset(value);
            BlockState blockState = world.getBlockState(offset);
            if (blockState.isAir()) {
                direction = value;
                continue;
            }
            if (blockState.isOf(Blocks.LEVER)) {
                // 拉杆没有附着在墙壁上，破坏拉杆
                BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
                if (blockState.get(WallMountedBlock.FACE) != BlockFace.WALL) {
                    breakManager.breakBlock(offset, Direction.DOWN, false);
                    return false;
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
                        breakManager.breakBlock(offset, Direction.DOWN, false);
                        return false;
                    }
                } else {
                    // 拉杆附着在了墙上，但不是当前要破坏的基岩方块
                    breakManager.breakBlock(offset, Direction.DOWN, false);
                    return false;
                }
            }
        }
        if (destructor.getLeverPos() != null) {
            return true;
        }
        if (direction == null) {
            return false;
        }
        // 没有正确的拉杆附着在基岩上，放置并激活拉杆
        BlockPos offset = bedrockPos.offset(direction);
        FakePlayerUtils.replenishment(fakePlayer, stack -> stack.isOf(Items.LEVER));
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        BlockHitResult hitResult = new BlockHitResult(bedrockPos.toCenterPos(), direction, bedrockPos, false);
        // 放置拉杆
        interactionManager.interactBlock(fakePlayer, world, fakePlayer.getMainHandStack(), Hand.MAIN_HAND, hitResult);
        // 再次单击激活拉杆
        interactionLever(fakePlayer, offset);
        destructor.setLeverPos(offset);
        return true;
    }

    /**
     * 破除基岩
     */
    private static StepResult pistonBreakBedrock(BedrockDestructor destructor, EntityPlayerMPFake fakePlayer) {
        BlockPos bedrockPos = destructor.getBedrockPos();
        BlockPos up = bedrockPos.up();
        // 基岩上方方块是活塞
        BlockState blockState = fakePlayer.getWorld().getBlockState(up);
        if (blockState.isOf(Blocks.PISTON)) {
            BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
            // 计算剩余挖掘时间
            int currentTime = breakManager.getCurrentBreakingTime(up);
            if (currentTime == 1) {
                // 方块将在本游戏刻挖掘完毕
                BlockPos leverPos = destructor.getLeverPos();
                // 关闭拉杆，然后放置朝下的活塞
                interactionLever(fakePlayer, leverPos);
                destructor.setLeverPos(null);
                // 继续挖掘，此时活塞应该会挖掘完毕
                breakManager.breakBlock(up, Direction.DOWN, false);
                // 放置一个朝下的活塞，这个活塞会破坏掉基岩
                placePiston(fakePlayer, bedrockPos, Direction.DOWN);
                return StepResult.COMPLETION;
            }
            breakManager.breakBlock(up, Direction.DOWN, false);
            return StepResult.TICK_COMPLETION;
        } else {
            return StepResult.COMPLETION;
        }
    }

    private static boolean cleanPiston(EntityPlayerMPFake fakePlayer, BlockPos blockPos) {
        BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
        if (fakePlayer.getWorld().getBlockState(blockPos).isAir()) {
            return true;
        }
        return breakManager.breakBlock(blockPos, Direction.DOWN, false);
    }

    /**
     * 放置活塞
     */
    private static void placePiston(EntityPlayerMPFake fakePlayer, BlockPos bedrockPos, Direction direction) {
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        // 看向与活塞相反的方向
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        FakePlayerUtils.replenishment(fakePlayer, itemStack -> itemStack.isOf(Items.PISTON));
        // 放置活塞
        BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(bedrockPos, 1.0), direction, bedrockPos.up(), false);
        interactionManager.interactBlock(fakePlayer, fakePlayer.getWorld(), fakePlayer.getMainHandStack(), Hand.MAIN_HAND, hitResult);
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
        TICK_COMPLETION
    }
}
