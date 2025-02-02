package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.block.*;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.PeriodicTaskUtils;
import org.carpetorgaddition.periodic.fakeplayer.actioncontext.FarmContext;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.function.Predicate;

public class FakePlayerFarm {
    public static void farm(FarmContext context, EntityPlayerMPFake fakePlayer) {
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            BlockPos cropPos = context.getCropPos();
            // 继续挖掘之前未挖掘完成的方块
            if (cropPos != null && !breakBlock(fakePlayer, cropPos, context)) {
                return;
            }
            // 根据副手的物品是什么来决定种植什么农作物
            ItemStack cropsItem = fakePlayer.getOffHandStack();
            // 获取当前种植的是什么类型的农作物
            FarmType farmType = FarmType.getFarmType(cropsItem);
            if (farmType == FarmType.NONE) {
                return;
            }
            // 获取玩家交互距离内的所有方块
            double range = fakePlayer.getBlockInteractionRange();
            // 限制交互距离，减少卡顿
            Box box = new Box(fakePlayer.getBlockPos()).expand(Math.min(range, 10.0));
            SelectionArea area = new SelectionArea(box);
            for (BlockPos blockPos : area) {
                if (fakePlayer.canInteractWithBlockAt(blockPos, 0)) {
                    if (tryFarm(fakePlayer, blockPos, farmType, cropsItem, context)) {
                        continue;
                    }
                    break;
                }
            }
        }
    }

    /**
     * 尝试种植农作物
     *
     * @return 是否应该继续本tick种植
     */
    private static boolean tryFarm(EntityPlayerMPFake fakePlayer, BlockPos blockPos, FarmType farmType, ItemStack cropsItem, FarmContext context) {
        return switch (farmType) {
            case CROPS -> plantingCrops(fakePlayer, cropsItem, blockPos, context);
            case BAMBOO -> plantingBamboo(fakePlayer, blockPos, context);
            default -> true;
        };
    }


    /**
     * 种植常规的农作物，小麦、土豆、胡萝卜，甜菜，以及火把花，瓶子草
     *
     * @return 是否需要继续循环
     */
    private static boolean plantingCrops(EntityPlayerMPFake fakePlayer, ItemStack itemStack, BlockPos blockPos, FarmContext context) {
        World world = fakePlayer.getWorld();
        if (!world.getBlockState(blockPos).isOf(Blocks.FARMLAND)) {
            return true;
        }
        PlayerScreenHandler screenHandler = fakePlayer.playerScreenHandler;
        // 种子是否足够
        boolean thereAreManySeeds = true;
        // 玩家手上的种子太少，需要补货
        if (itemStack.getCount() <= 1 && !fakePlayer.isCreative()) {
            Predicate<ItemStack> predicate = stack -> ItemStack.areItemsAndComponentsEqual(itemStack, stack);
            // 尝试补货
            thereAreManySeeds = replenishment(fakePlayer, screenHandler.slots.size() - 1, predicate, screenHandler);
        }
        BlockPos upPos = blockPos.up();
        BlockState blockState = world.getBlockState(upPos);
        // 如果耕地上方方块是空气，种植农作物
        if (thereAreManySeeds && blockState.isAir()) {
            // 种植农作物
            plant(fakePlayer, world, itemStack, blockPos, upPos);
        }
        // 种植农作物后，收集或催熟
        Block block = blockState.getBlock();
        // 处理普通的农作物
        if (block instanceof CropBlock cropBlock) {
            // 农作物已经成熟，收集农作物，火把花不能直接用isMature方法判断是否成熟
            if (cropBlock.isMature(blockState) && !(cropBlock instanceof TorchflowerBlock)) {
                // 收集农作物（破坏方块）
                return breakBlock(fakePlayer, upPos, context);
            } else {
                fertilize(fakePlayer, screenHandler, world, upPos);
            }
        } else if (block instanceof PitcherCropBlock pitcherCropBlock) {
            // 处理瓶子草
            // 判断瓶子草是否可以施肥，如果可以，就施肥，否则瓶子草可能已经成熟，破坏瓶子草
            if (pitcherCropBlock.isFertilizable(world, upPos, blockState)) {
                // 施肥
                fertilize(fakePlayer, screenHandler, world, upPos);
            } else {
                // 收集瓶子草
                return breakBlock(fakePlayer, upPos, context);
            }
        } else if (block == Blocks.TORCHFLOWER) {
            // 收集火把花
            return breakBlock(fakePlayer, upPos, context);
        }
        return true;
    }

    // 种植竹子
    private static boolean plantingBamboo(EntityPlayerMPFake fakePlayer, BlockPos plantablePos, FarmContext context) {
        World world = fakePlayer.getWorld();
        // 是否可以种植竹子
        if (!world.getBlockState(plantablePos).isIn(BlockTags.BAMBOO_PLANTABLE_ON)
                // 竹子和竹笋自身也有“bamboo_plantable_on”标签，需要排除掉
                || world.getBlockState(plantablePos).isOf(Blocks.BAMBOO)
                || world.getBlockState(plantablePos).isOf(Blocks.BAMBOO_SAPLING)) {
            return true;
        }
        // 排除埋在底下的可种植方块
        if (!world.getBlockState(plantablePos.up()).isAir()
                && !world.getBlockState(plantablePos.up()).isOf(Blocks.BAMBOO)
                && !world.getBlockState(plantablePos.up()).isOf(Blocks.BAMBOO_SAPLING)) {
            return true;
        }
        PlayerScreenHandler playerScreenHandler = fakePlayer.playerScreenHandler;
        BlockPos bambooPos = plantablePos.up();
        BlockState blockState = world.getBlockState(bambooPos);
        if (blockState.isAir()) {
            // 不去主动种植竹子，只催熟和收割现有的竹子
            return true;
        }
        Block block = blockState.getBlock();
        if (block instanceof BambooShootBlock && world.getBlockState(bambooPos.up()).isAir()) {
            // 竹笋方块，如果上方没有方块阻挡，直接使用骨粉
            fertilize(fakePlayer, playerScreenHandler, world, bambooPos);
        } else if (block instanceof BambooBlock bambooBlock) {
            // 判断竹子是否可以施肥
            if (bambooBlock.isFertilizable(world, bambooPos, blockState)) {
                // 可以施肥
                // 竹子上方第一个空气方块开始，向上空气方块的数量
                int airCount = 0;
                // 一个标记，从这个标记变为true开始，记录上方空气的数量
                boolean hasAir = false;
                /*
                 * 从当前竹子根的位置向上找16格，判断上方是否有上次砍伐但没来得及掉落的竹子。
                 * 竹子被砍断后不会立即掉落所有的竹子，而且从砍断的位置开始向上逐个掉落，
                 * 如果在掉落前立即撒骨粉施肥，那么新的竹子极有可能与之前的竹子连接，之前的竹子不会掉落，会白白浪费骨粉。
                 * 对竹子使用骨粉时会让竹子向上生长1-2格，所以，要想让新的竹子不会与旧的竹子相连接，新竹子距离之前的竹子至少要距离3格
                 * 从第二格开始找是因为竹子是从第二格开始砍断的，第零格是支撑竹子的方块，第一格是竹子的根，所以底下这两格一定不是空气。
                 */
                for (int height = 2; height <= 16; height++) {
                    BlockState tempBlockState = world.getBlockState(plantablePos.up(height));
                    if (tempBlockState.isAir()) {
                        hasAir = true;
                        airCount++;
                    } else if (!tempBlockState.isOf(Blocks.BAMBOO)) {
                        // 有方块阻止了竹子生长
                        break;
                    }
                    if (hasAir) {
                        // 如果上方连续的空气方块数量大于等于3，则可以使用骨粉
                        if (airCount >= 3) {
                            fertilize(fakePlayer, playerScreenHandler, world, bambooPos);
                            break;
                        } else if (tempBlockState.isOf(Blocks.BAMBOO)) {
                            // 如果上方连续的空气方块数量小于3，不能施肥，跳出循环
                            break;
                        }
                    }
                    if (height == 16) {
                        /*
                         * 检查到了第16格，直接施肥
                         * 如果第15格是空气，那么判断第16格时：
                         * 1.如果第16格是竹子，则代码会在上面检查airCount>=3时，条件不会成立，会进入else if判断然后跳出循环，代码不会执行到这里
                         * 2.如果第16格是空气，那么15格16格是空气，17格超出了竹子的最大生长高度所以一定也是空气，连续3格空气，可以施肥。
                         */
                        fertilize(fakePlayer, playerScreenHandler, world, bambooPos);
                    }
                }
            } else {
                // 竹子已生长到最大高度，破坏竹子
                return useToolBreakBlock(fakePlayer, bambooPos.up(), context);
            }
        }
        return true;
    }

    // 种植
    private static void plant(EntityPlayerMPFake fakePlayer, World world, ItemStack itemStack, BlockPos blockPos, BlockPos upPos) {
        // 让假玩家看向该位置（这不是必须的）
        fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, upPos.toCenterPos());
        BlockHitResult hitResult = new BlockHitResult(blockPos.toCenterPos(), Direction.UP, upPos, false);
        fakePlayer.interactionManager.interactBlock(fakePlayer, world, itemStack, Hand.OFF_HAND, hitResult);
        // 摆动手
        fakePlayer.swingHand(Hand.OFF_HAND, true);
    }

    // 撒骨粉催熟
    private static void fertilize(EntityPlayerMPFake fakePlayer, PlayerScreenHandler screenHandler, World world, BlockPos upPos) {
        Predicate<ItemStack> predicate = stack -> stack.isOf(Items.BONE_MEAL);
        // 要求玩家身上有骨粉
        if (replenishment(fakePlayer, predicate)) {
            ItemStack itemStack = fakePlayer.getMainHandStack();
            if (itemStack.getCount() > 1
                    || fakePlayer.isCreative()
                    || replenishment(fakePlayer, fakePlayer.getInventory().selectedSlot + 36, predicate, screenHandler)) {
                // 如果手上有多余一个的骨粉，就使用骨粉
                Vec3d centerPos = upPos.toCenterPos();
                // 让假玩家看向该位置（这不是必须的）
                fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, centerPos);
                // 使用骨粉
                BlockHitResult hitResult = new BlockHitResult(centerPos, Direction.DOWN, upPos, true);
                fakePlayer.interactionManager.interactBlock(fakePlayer, world, itemStack, Hand.MAIN_HAND, hitResult);
                // 摆动手
                fakePlayer.swingHand(Hand.MAIN_HAND, true);
            }
        }
    }


    /**
     * 收集农作物，创造模式下不会受限于方块挖掘冷却
     *
     * @return 是否完成挖掘
     */
    private static boolean breakBlock(EntityPlayerMPFake fakePlayer, BlockPos pos, FarmContext context) {
        // 让假玩家看向该位置（这不是必须的）
        fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, pos.toCenterPos());
        BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
        boolean breakBlock = breakManager.breakBlock(pos, Direction.DOWN, !fakePlayer.isCreative());
        context.setCropPos(breakBlock ? null : pos);
        return breakBlock;
    }


    /**
     * 使用工具破坏硬度大于0的方块
     *
     * @return 是否完成挖掘
     */
    private static boolean useToolBreakBlock(EntityPlayerMPFake fakePlayer, BlockPos pos, FarmContext context) {
        // 如果玩家是创造模式，或者玩家手上的工具已经能够瞬间破坏方块，那么就无需切换工具，直接破坏方块即可
        replenishment(fakePlayer, itemStack -> itemStack.getItem() instanceof ToolItem);
        // 让假玩家看向该位置（这不是必须的）
        fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, pos.toCenterPos());
        BlockBreakManager breakManager = PeriodicTaskUtils.getBlockBreakManager(fakePlayer);
        boolean breakBlock = breakManager.breakBlock(pos, Direction.DOWN);
        context.setCropPos(breakBlock ? null : pos);
        return breakBlock;
    }

    // 自动补货，返回值表示是否补货成功
    private static boolean replenishment(EntityPlayerMPFake fakePlayer, int slotIndex, Predicate<ItemStack> predicate, PlayerScreenHandler screenHandler) {
        DefaultedList<Slot> slots = screenHandler.slots;
        // 遍历玩家物品栏，找到需要的物品
        for (int index = 5; index < slots.size() - 1; index++) {
            if (index == slotIndex) {
                continue;
            }
            ItemStack itemStack = slots.get(index).getStack();
            // 找到了，就移动到指定槽位
            if (predicate.test(itemStack)) {
                // 如果物品的堆叠数已经是最大值，就移动一半，否则移动所有
                if (itemStack.getCount() == itemStack.getMaxCount()) {
                    FakePlayerUtils.pickupAndMoveHalfItemStack(screenHandler, index, slotIndex, fakePlayer);
                } else {
                    FakePlayerUtils.pickupAndMoveItemStack(screenHandler, index, slotIndex, fakePlayer);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 将合适的物品移动到主手
     *
     * @return 是否移动成功
     */
    private static boolean replenishment(EntityPlayerMPFake fakePlayer, Predicate<ItemStack> predicate) {
        if (predicate.test(fakePlayer.getMainHandStack())) {
            return true;
        }
        PlayerScreenHandler screenHandler = fakePlayer.playerScreenHandler;
        // 主手槽位
        int mainHeadSlot = 36 + fakePlayer.getInventory().selectedSlot;
        for (int i = 9; i < 45; i++) {
            if (i == mainHeadSlot) {
                continue;
            }
            if (predicate.test(screenHandler.getSlot(i).getStack())) {
                FakePlayerUtils.swapSlotItem(screenHandler, i, mainHeadSlot, fakePlayer);
                return true;
            }
        }
        return false;
    }

    public enum FarmType {
        /**
         * 种植普通农作物，小麦、土豆、胡萝卜，甜菜，以及火把花，瓶子草
         */
        CROPS,
        /**
         * 种植竹子
         */
        BAMBOO,
        /**
         * 种植可可豆
         */
        @SuppressWarnings("unused")
        COCOA,
        /**
         * 一个占位符，表示什么都不种植
         */
        NONE;

        public static FarmType getFarmType(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return NONE;
            }
            if ((itemStack.isOf(Items.WHEAT_SEEDS)
                    || itemStack.isOf(Items.POTATO)
                    || itemStack.isOf(Items.CARROT)
                    || itemStack.isOf(Items.BEETROOT_SEEDS)
                    || itemStack.isOf(Items.TORCHFLOWER_SEEDS)
                    || itemStack.isOf(Items.PITCHER_POD))) {
                return CROPS;
            }
            if (itemStack.isOf(Items.BAMBOO)) {
                return BAMBOO;
            }
            return NONE;
        }
    }
}
