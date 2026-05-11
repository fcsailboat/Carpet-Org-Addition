package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.util.ServerUtils;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockExcavator {
    private final EntityPlayerMPFake player;
    /**
     * 方块挖掘冷却
     */
    private int blockBreakingCooldown;
    /**
     * 当前正在挖掘的方块坐标
     */
    @Nullable
    private BlockPos currentBreakingPos;
    /**
     * 当前方块的挖掘进度，在0-1之间
     */
    private float currentBreakingProgress;

    public BlockExcavator(EntityPlayerMPFake player) {
        this.player = player;
    }

    public void tick() {
        if (this.blockBreakingCooldown > 0) {
            this.blockBreakingCooldown--;
        }
    }

    public boolean mining(BlockPos blockPos, Direction direction) {
        return mining(blockPos, direction, true);
    }

    /**
     * 尝试挖掘方块
     *
     * @param blockPos         挖掘方块的位置
     * @param breakingCooldown 是否受方块挖掘冷却影响
     * @return 是否成功挖掘
     */
    public boolean mining(BlockPos blockPos, Direction direction, boolean breakingCooldown) {
        // 方块挖掘冷却
        if (breakingCooldown && this.blockBreakingCooldown > 0) {
            return false;
        }
        Level world = ServerUtils.getWorld(this.player);
        ServerPlayerGameMode interactionManager = this.player.gameMode;
        GameType gameMode = interactionManager.getGameModeForPlayer();
        // 当前方块是可以破坏的
        if (this.player.blockActionRestricted(world, blockPos, gameMode)) {
            return false;
        }
        // 当前位置是否为出生点保护区域或超出了世界边界
        if (!world.mayInteract(this.player, blockPos)) {
            return false;
        }
        // 正在挖掘空气方块
        if (this.currentBreakingPos != null && world.getBlockState(this.currentBreakingPos).isAir()) {
            this.currentBreakingPos = null;
            return true;
        }
        BlockState blockState = world.getBlockState(blockPos);
        // 让假玩家看向该位置（这不是必须的）
        this.player.lookAt(EntityAnchorArgument.Anchor.EYES, ServerUtils.getBlockCenter(blockPos));
        // 获取每次挖掘增加的进度
        float delta = blockState.getDestroyProgress(this.player, world, blockPos);
        // 当前方块是否被破坏
        boolean blockBroken;
        if (this.player.isCreative()) {
            // 创造模式下瞬间破坏方块
            this.breakingAction(Action.START_DESTROY_BLOCK, blockPos, direction);
            this.blockBreakingCooldown = 5;
            blockBroken = true;
        } else if (this.currentBreakingPos == null) {
            blockBroken = startMining(blockPos, direction, delta);
        } else if (this.currentBreakingPos.equals(blockPos)) {
            blockBroken = continueMining(blockPos, direction, delta);
        } else {
            // 当前挖掘位置与之前的挖掘位置不一致，中断挖掘
            this.breakingAction(Action.ABORT_DESTROY_BLOCK, this.currentBreakingPos, direction);
            // 重新开始挖掘
            blockBroken = startMining(blockPos, direction, delta);
        }
        // 更新上次操作时间
        this.player.resetLastActionTime();
        // 摆动手
        this.player.swing(InteractionHand.MAIN_HAND);
        return blockBroken;
    }

    /**
     * 开始挖掘方块
     *
     * @return 是否瞬间破坏
     */
    private boolean startMining(BlockPos blockPos, Direction direction, float delta) {
        // 开始挖掘方块
        this.breakingAction(Action.START_DESTROY_BLOCK, blockPos, direction);
        if (delta >= 1F) {
            // 瞬间破坏
            return true;
        } else {
            this.currentBreakingPos = blockPos;
            this.currentBreakingProgress = 0F;
        }
        return false;
    }

    /**
     * 继续挖掘方块
     *
     * @return 是否完成挖掘
     */
    private boolean continueMining(BlockPos blockPos, Direction direction, float delta) {
        this.currentBreakingProgress += delta;
        if (this.currentBreakingProgress >= 1F) {
            // 破坏方块
            this.breakingAction(Action.STOP_DESTROY_BLOCK, blockPos, direction);
            this.currentBreakingPos = null;
            this.blockBreakingCooldown = 5;
            this.currentBreakingProgress = 0F;
            return true;
        }
        return false;
    }

    /**
     * 如果返回1，表示该方块可以在当前游戏刻破坏
     *
     * @return 破坏当前方块还需要多少个游戏刻
     */
    public int computingRemainingMiningTime(BlockPos blockPos) {
        Level world = ServerUtils.getWorld(this.player);
        if (this.player.isCreative()) {
            return 1;
        }
        BlockState blockState = world.getBlockState(blockPos);
        float delta = blockState.getDestroyProgress(this.player, world, blockPos);
        return (int) Math.ceil((1F - this.currentBreakingProgress) / delta);
    }

    private void breakingAction(Action action, BlockPos blockPos, Direction direction) {
        Level world = ServerUtils.getWorld(this.player);
        this.player.gameMode.handleBlockBreakAction(blockPos, action, direction, world.getMaxY(), -1);
    }

    public EntityPlayerMPFake getPlayer() {
        return this.player;
    }

    /**
     * @return 玩家是否可以破坏指定位置的方块
     */
    public boolean canBreak(BlockPos blockPos) {
        Level world = ServerUtils.getWorld(this.player);
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        // 非管理员不能破坏管理员方块
        if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
            return false;
        }
        // 是否限制了方块破坏
        if (this.player.blockActionRestricted(world, blockPos, this.player.gameMode.getGameModeForPlayer())) {
            return false;
        }
        ItemStack mainHandItemStack = this.player.getMainHandItem();
        Item mainHandItem = mainHandItemStack.getItem();
        // 主手物品是否可以挖掘方块，当前位置是否超出了世界边界
        if (mainHandItem.canDestroyBlock(mainHandItemStack, blockState, world, blockPos, this.player) && world.mayInteract(this.player, blockPos)) {
            // 创造模式破坏方块
            if (this.player.isCreative() || blockState.isAir()) {
                return true;
            }
            // 非创造玩家无法破坏硬度为-1的方块
            return blockState.getDestroySpeed(world, blockPos) != -1;
        }
        return true;
    }
}
