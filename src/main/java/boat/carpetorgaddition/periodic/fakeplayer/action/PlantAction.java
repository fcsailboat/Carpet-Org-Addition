package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public class PlantAction extends AbstractPlayerAction {
    /**
     * 当前正在采集的农作物
     */
    private BlockPos cropPos;
    private PlayerStorageInventory inventory;
    private BlockExcavator excavator;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("plant");

    public PlantAction(EntityPlayerMPFake fakePlayer) {
        super(fakePlayer);
    }

    @Override
    protected void tick() {
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            // 继续挖掘之前未挖掘完成的方块
            if (this.cropPos != null && !breakBlock(this.cropPos)) {
                return;
            }
            // 根据副手的物品是什么来决定种植什么农作物
            ItemStack cropsItem = this.getFakePlayer().getOffhandItem();
            // 获取当前种植的是什么类型的农作物
            CropType cropType = CropType.getCropType(cropsItem);
            if (cropType == CropType.NONE) {
                return;
            }
            // 获取玩家交互距离内的所有方块
            double range = this.getFakePlayer().blockInteractionRange();
            // 限制交互距离，减少卡顿
            AABB box = new AABB(this.getFakePlayer().blockPosition()).inflate(Math.min(range, 10.0));
            for (BlockPos blockPos : new BlockPosTraverser(box)) {
                if (this.getFakePlayer().isWithinBlockInteractionRange(blockPos, 0)) {
                    if (this.tryPlanting(blockPos, cropType, cropsItem)) {
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
    private boolean tryPlanting(BlockPos blockPos, CropType cropType, ItemStack cropsItem) {
        return switch (cropType) {
            case CROPS -> this.plantCrops(cropsItem, blockPos);
            case NETHER_WART -> this.plantNetherWart(blockPos);
            case MELON -> this.plantMelon(blockPos, cropsItem);
            case BAMBOO -> this.plantBamboo(blockPos);
            case NONE -> false;
        };
    }

    private boolean plantMelon(BlockPos farmlandPos, ItemStack seedItem) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerLevel world = ServerUtils.getWorld(fakePlayer);
        if (!world.getBlockState(farmlandPos).is(BlockTags.SUPPORTS_STEM_CROPS)) {
            return true;
        }
        BlockPos up = farmlandPos.above();
        BlockState blockState = world.getBlockState(up);
        if (seedItem.is(Items.MELON_SEEDS)) {
            return this.plantMelon(blockState, up, world, Blocks.MELON, Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM);
        } else if (seedItem.is(Items.PUMPKIN_SEEDS)) {
            return this.plantMelon(blockState, up, world, Blocks.PUMPKIN, Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
        } else {
            return true;
        }
    }

    private boolean plantMelon(BlockState blockState, BlockPos stemPos, ServerLevel world, Block melon, Block stem, Block attachedStem) {
        if (blockState.is(attachedStem)) {
            Direction direction = blockState.getValue(AttachedStemBlock.FACING);
            BlockPos melonPos = stemPos.relative(direction);
            if (world.getBlockState(melonPos).is(melon)) {
                this.inventory.switchToAppropriateTool(world, melonPos);
                return this.breakBlock(melonPos);
            }
        } else if (blockState.is(stem) && blockState.getValue(StemBlock.AGE) < StemBlock.MAX_AGE) {
            this.fertilize(world, stemPos);
        }
        return true;
    }

    /**
     * 种植常规的农作物，小麦、土豆、胡萝卜，甜菜，以及火把花，瓶子草
     *
     * @return 是否需要继续循环
     */
    private boolean plantCrops(ItemStack itemStack, BlockPos blockPos) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        Level world = ServerUtils.getWorld(fakePlayer);
        if (!world.getBlockState(blockPos).is(BlockTags.SUPPORTS_CROPS)) {
            return true;
        }
        BlockPos upPos = blockPos.above();
        BlockState blockState = world.getBlockState(upPos);
        // 如果耕地上方方块是空气，种植农作物
        if ((fakePlayer.isCreative() || this.inventory.replenish(InteractionHand.OFF_HAND, 1)) && blockState.isAir()) {
            // 种植农作物
            plant(world, itemStack, blockPos, upPos);
        }
        // 种植农作物后，收集或催熟
        Block block = blockState.getBlock();
        // 处理普通的农作物
        if (block instanceof CropBlock cropBlock) {
            // 农作物已经成熟，收集农作物，火把花不能直接用isMature方法判断是否成熟
            if (cropBlock.isMaxAge(blockState) && !(cropBlock instanceof TorchflowerCropBlock)) {
                // 收集农作物（破坏方块）
                return this.breakBlock(upPos);
            } else {
                this.fertilize(world, upPos);
            }
        } else if (block instanceof PitcherCropBlock pitcherCropBlock) {
            // 处理瓶子草
            // 判断瓶子草是否可以施肥，如果可以，就施肥，否则瓶子草可能已经成熟，破坏瓶子草
            if (pitcherCropBlock.isValidBonemealTarget(world, upPos, blockState)) {
                // 施肥
                this.fertilize(world, upPos);
            } else {
                // 收集瓶子草
                return this.breakBlock(upPos);
            }
        } else if (block == Blocks.TORCHFLOWER) {
            // 收集火把花
            return this.breakBlock(upPos);
        }
        return true;
    }

    /**
     * 种植下界疣
     */
    private boolean plantNetherWart(BlockPos soulSandPos) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerLevel world = ServerUtils.getWorld(fakePlayer);
        if (!world.getBlockState(soulSandPos).is(BlockTags.SUPPORTS_NETHER_WART)) {
            return true;
        }
        BlockPos above = soulSandPos.above();
        if (fakePlayer.isCreative() || this.inventory.replenish(InteractionHand.OFF_HAND, 1)) {
            if (world.getBlockState(above).isAir()) {
                this.plant(world, fakePlayer.getOffhandItem(), soulSandPos, above);
            }
        }
        BlockState blockState = world.getBlockState(above);
        if (blockState.is(Blocks.NETHER_WART) && blockState.getValue(NetherWartBlock.AGE) == 3) {
            return this.breakBlock(above);
        }
        return true;
    }

    // 种植竹子
    private boolean plantBamboo(BlockPos plantablePos) {
        Level world = ServerUtils.getWorld(this.getFakePlayer());
        // 是否可以种植竹子（仅检查，稍后不会种植竹子）
        if (!world.getBlockState(plantablePos).is(BlockTags.SUPPORTS_BAMBOO)
            // 竹子和竹笋自身也有“supports_bamboo”标签，需要排除掉
            || world.getBlockState(plantablePos).is(Blocks.BAMBOO)
            || world.getBlockState(plantablePos).is(Blocks.BAMBOO_SAPLING)) {
            return true;
        }
        BlockPos bambooPos = plantablePos.above();
        BlockState blockState = world.getBlockState(bambooPos);
        if (blockState.isAir()) {
            // 不去主动种植竹子，只催熟和收割现有的竹子
            return true;
        }
        Block block = blockState.getBlock();
        if (block instanceof BambooSaplingBlock && world.getBlockState(bambooPos.above()).isAir()) {
            // 竹笋方块，如果上方没有方块阻挡，直接使用骨粉
            this.fertilize(world, bambooPos);
        } else if (block instanceof BambooStalkBlock bambooBlock) {
            // 判断竹子是否可以施肥
            if (bambooBlock.isValidBonemealTarget(world, bambooPos, blockState)) {
                // 竹子上方第一个空气方块开始，向上空气方块的数量
                int airCount = 0;
                // 一个标记，从这个标记变为true开始，记录上方空气的数量
                boolean hasAir = false;
                /*
                 * 从当前竹子根的位置向上找16格，判断上方是否有上次砍伐但没来得及掉落的竹子。
                 * 竹子被砍断后不会立即掉落所有的竹子，而且从砍断的位置开始向上逐个掉落，
                 * 如果在掉落前立即撒骨粉施肥，那么新的竹子极有可能与之前的竹子连接，之前的竹子不会掉落，会白白浪费骨粉。
                 * 对竹子使用骨粉时会让竹子向上生长1-2格，所以，要想让新的竹子不会与之前的竹子相连接，新竹子距离之前的竹子至少要距离3格
                 * 从第二格开始找是因为竹子是从第二格开始砍断的，第零格是支撑竹子的方块，第一格是竹子的根，所以底下这两格一定不是空气。
                 */
                for (int height = 2; height <= 16; height++) {
                    BlockState tempBlockState = world.getBlockState(plantablePos.above(height));
                    if (tempBlockState.isAir()) {
                        hasAir = true;
                        airCount++;
                    } else if (!tempBlockState.is(Blocks.BAMBOO)) {
                        // 有方块阻止了竹子生长
                        break;
                    }
                    if (hasAir) {
                        // 如果上方连续的空气方块数量大于等于3，则可以使用骨粉
                        if (airCount >= 3) {
                            this.fertilize(world, bambooPos);
                            break;
                        } else if (tempBlockState.is(Blocks.BAMBOO)) {
                            // 如果上方连续的空气方块数量小于3，不能施肥，跳出循环
                            break;
                        }
                    }
                    if (height == 16) {
                        /*
                         * 检查到了第16格，直接施肥
                         * 如果第15格是空气，那么判断第16格时：
                         * 1.如果第16格是竹子，则代码会在上面检查airCount>=3时，条件不会成立，会进入else if判断然后跳出循环，代码不会执行到这里
                         * 2.如果第16格是空气，那么第15格和第16格是空气，第17格超出了竹子的最大生长高度所以也认为是空气（即使不是空气，第17格的方块
                         *   也不会对竹子的生长产生影响，因为竹子不会生长到第17格），连续3格空气，可以施肥。
                         */
                        this.fertilize(world, bambooPos);
                    }
                }
            } else {
                // 竹子已生长到最大高度，破坏竹子
                return useToolBreakBlock(world, bambooPos.above());
            }
        }
        return true;
    }

    // 种植
    private void plant(Level world, ItemStack itemStack, BlockPos farmlandPos, BlockPos cropPos) {
        // 让假玩家看向该位置（这不是必须的）
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        fakePlayer.lookAt(EntityAnchorArgument.Anchor.EYES, cropPos.getCenter());
        BlockHitResult hitResult = new BlockHitResult(farmlandPos.getCenter(), Direction.UP, cropPos, false);
        fakePlayer.gameMode.useItemOn(fakePlayer, world, itemStack, InteractionHand.OFF_HAND, hitResult);
        // 摆动手
        ServerUtils.swing(fakePlayer, InteractionHand.OFF_HAND);
    }

    // 撒骨粉催熟
    private void fertilize(Level world, BlockPos cropPos) {
        Predicate<ItemStack> predicate = stack -> stack.is(Items.BONE_MEAL);
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        // 补货执行了两次，第一次是将骨粉物品移动到主手，第二次是检查骨粉是否只有一个，如果只有一个，则继续从物品栏中获取骨粉并移动到主手，但如果假玩家是创造模式，则不需要再次补货
        // 保留一个骨粉是为了更方便的捡起地上的骨粉
        if (this.inventory.replenish(predicate) && (fakePlayer.isCreative() || this.inventory.replenish(1))) {
            ItemStack itemStack = fakePlayer.getMainHandItem();
            Vec3 centerPos = cropPos.getCenter();
            // 让假玩家看向该位置（这不是必须的）
            fakePlayer.lookAt(EntityAnchorArgument.Anchor.EYES, centerPos);
            // 使用骨粉
            BlockHitResult hitResult = new BlockHitResult(centerPos, Direction.DOWN, cropPos, true);
            fakePlayer.gameMode.useItemOn(fakePlayer, world, itemStack, InteractionHand.MAIN_HAND, hitResult);
            // 摆动手
            ServerUtils.swing(fakePlayer, InteractionHand.MAIN_HAND);
        }
    }

    /**
     * 收集农作物，创造模式下不会受限于方块挖掘冷却
     *
     * @return 是否完成挖掘
     */
    private boolean breakBlock(BlockPos cropPos) {
        boolean breakBlock = this.excavator.mining(cropPos, Direction.DOWN, !this.getFakePlayer().isCreative());
        this.cropPos = breakBlock ? null : cropPos;
        return breakBlock;
    }

    /**
     * 使用工具破坏硬度大于0的方块
     *
     * @return 是否完成挖掘
     */
    private boolean useToolBreakBlock(Level world, BlockPos cropPos) {
        // 如果有工具，拿在主手，剑可以瞬间破坏竹子，它也是工具物品
        this.inventory.switchToAppropriateTool(world, cropPos);
        return breakBlock(cropPos);
    }

    @Override
    public List<Component> info() {
        return List.of(this.getInfoLocalizationKey().translate(this.getFakePlayer().getDisplayName()));
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.PLANT;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    protected void onAssignPlayer() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        this.inventory = PlayerStorageInventory.of(fakePlayer);
        this.excavator = PlayerComponentCoordinator.of(fakePlayer).getBlockExcavator();
    }

    @Override
    protected void onClearPlayer() {
        this.inventory = null;
        this.excavator = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        return this.getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    public enum CropType {
        /**
         * 种植普通农作物，小麦、土豆、胡萝卜，甜菜，以及火把花，瓶子草
         */
        CROPS,
        /**
         * 种植下界疣
         */
        NETHER_WART,
        /**
         * 种植竹子
         */
        BAMBOO,
        /**
         * 种植西瓜和南瓜
         */
        MELON,
        /**
         * 一个占位符，表示什么都不种植
         */
        NONE;

        public static CropType getCropType(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return NONE;
            }
            if ((itemStack.is(Items.WHEAT_SEEDS)
                 || itemStack.is(Items.POTATO)
                 || itemStack.is(Items.CARROT)
                 || itemStack.is(Items.BEETROOT_SEEDS)
                 || itemStack.is(Items.TORCHFLOWER_SEEDS)
                 || itemStack.is(Items.PITCHER_POD))) {
                return CROPS;
            }
            if (itemStack.is(Items.NETHER_WART)) {
                return NETHER_WART;
            }
            if (itemStack.is(Items.BAMBOO)) {
                return BAMBOO;
            }
            if (itemStack.is(Items.MELON_SEEDS) || itemStack.is(Items.PUMPKIN_SEEDS)) {
                return MELON;
            }
            return NONE;
        }
    }
}
