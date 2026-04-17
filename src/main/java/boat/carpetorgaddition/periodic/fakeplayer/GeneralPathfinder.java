package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.network.s2c.FakePlayerPathfinderS2CPacket;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class GeneralPathfinder implements FakePlayerPathfinder {
    private final Supplier<EntityPlayerMPFake> fakePlayerSupplier;
    private final Supplier<Optional<BlockPos>> target;
    private final ArrayList<Vec3> nodes = new ArrayList<>();
    private int currentIndex;
    /**
     * 无需改变移动方向的时间
     */
    private int directTravelTime = 0;
    /**
     * 更新路径剩余游戏刻
     */
    private int updateTime;
    /**
     * 剩余潜行时间
     */
    private int sneakTime;
    /**
     * 剩余暂停时间
     */
    private int pauseTime;
    /**
     * 是否没有路能到达目标位置
     */
    private boolean noRoad = false;
    private BlockPos previous;
    /**
     * 重试寻路的次数
     */
    private int retryCount;
    private static final int FOLLOW_RANGE = 48;

    public GeneralPathfinder(Supplier<@NotNull EntityPlayerMPFake> fakePlayerSupplier, Supplier<Optional<BlockPos>> targetSupplier) {
        this.target = targetSupplier;
        this.fakePlayerSupplier = fakePlayerSupplier;
        this.pathfinding();
    }

    @Override
    public void tick() {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) getFakePlayer()).getActionPack();
        if (this.sneakTime > 0) {
            this.sneakTime--;
        }
        if (this.pauseTime > 0) {
            this.pauseTime--;
        }
        actionPack.setSneaking(this.sneakTime > 0);
        Optional<BlockPos> optional = this.target.get();
        if (optional.isEmpty()) {
            if (this.isFinished()) {
                return;
            }
            this.stop();
            return;
        }
        BlockPos blockPos = optional.get();
        if (!Objects.equals(this.previous, blockPos)) {
            // 更新目标位置
            this.pathfinding();
            this.setValid();
        }
        this.previous = blockPos;
        Vec3 pos = ServerUtils.getFootPos(this.getFakePlayer());
        this.directTravelTime--;
        if (this.updateTime > 0) {
            this.updateTime--;
        }
        // 是否还没有接近目标
        boolean farAway = blockPos.getBottomCenter().distanceTo(pos) > 1.5;
        if (this.updateTime == 0 && farAway) {
            // 更新路径
            this.noRoad = false;
            this.pathfinding();
            this.directTravelTime = 2;
        }
        if (this.isFinished()) {
            if (farAway) {
                // 已经无路可走了，避免每个游戏刻都查询路径
                if (this.noRoad) {
                    return;
                }
                // 没有到达目标位置，立即更新路径
                this.pathfinding();
            }
            return;
        }
        Vec3 current = this.getCurrentNode();
        boolean onGround = this.getFakePlayer().onGround();
        if (onGround) {
            if (this.directTravelTime <= 0) {
                this.getFakePlayer().lookAt(EntityAnchorArgument.Anchor.FEET, current);
            }
        } else if (this.getFakePlayer().getDeltaMovement().y() < 0) {
            // 玩家跳跃时，也会执行到这里
            // 玩家在从一格高的方块上下来，有时会尝试回到上一个节点
            this.directTravelTime = 1;
            // 如果下一个位置需要跳下去，设置潜行
            EntityPlayerMPFake fakePlayer = this.getFakePlayer();
            Direction direction = fakePlayer.getMotionDirection();
            BlockPos down = fakePlayer.blockPosition().below();
            Level world = ServerUtils.getWorld(fakePlayer);
            BlockState blockState = world.getBlockState(down);
            if (blockState.isAir() || blockState.isFaceSturdy(world, down, Direction.UP)) {
                BlockPos offset = down.relative(direction);
                if (!world.getBlockState(offset).isFaceSturdy(world, offset, Direction.UP)) {
                    this.sneakTime = 3;
                }
            }
        }
        // 玩家到达了当前节点
        if (this.arrivedAtAnyNode()) {
            return;
        }
        if (this.backToBeforeNode()) {
            this.retryCount++;
        }
        actionPack.setForward(1F);
        if (onGround) {
            this.jump(current, pos);
        }
    }

    private void jump(Vec3 current, Vec3 pos) {
        double horizontal = MathUtils.horizontalDistance(current, pos);
        double vertical = MathUtils.verticalDistance(current, pos);
        // 玩家可以直接走向方块，不需要跳跃
        if (vertical <= getFakePlayer().getAttributeValue(Attributes.STEP_HEIGHT)) {
            return;
        }
        // 当前位置比玩家位置低，不需要跳跃
        if (current.y() < pos.y()) {
            return;
        }
        // 跳跃高度可能受多种因素影响，但这里不考虑它
        if (horizontal < 1.0 && vertical < 1.25) {
            this.getFakePlayer().jumpFromGround();
        }
    }

    @Override
    public double length() {
        if (this.nodes.size() <= 1) {
            return 0.0;
        }
        return this.nodes.getFirst().distanceTo(this.nodes.getLast());
    }

    @Override
    public void pause(int time) {
        this.pauseTime = time;
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) getFakePlayer()).getActionPack();
        actionPack.setForward(0);
    }

    @Override
    public void pathfinding() {
        this.retryCount++;
        this.updateTime = 100;
        this.nodes.clear();
        this.currentIndex = 0;
        Optional<BlockPos> optional = this.target.get();
        if (optional.isEmpty()) {
            return;
        }
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        BlockPos blockPos = fakePlayer.blockPosition();
        BlockPos from = blockPos.offset(-FOLLOW_RANGE, -FOLLOW_RANGE, -FOLLOW_RANGE);
        BlockPos to = blockPos.offset(FOLLOW_RANGE, FOLLOW_RANGE, FOLLOW_RANGE);
        Level world = ServerUtils.getWorld(fakePlayer);
        PathNavigationRegion chunkCache = new PathNavigationRegion(world, from, to);
        WalkNodeEvaluator maker = new WalkNodeEvaluator();
        Vec3 pos = ServerUtils.getFootPos(fakePlayer);
        DummyEntity entity = new DummyEntity(world, pos);
        PathFinder navigator = new PathFinder(maker, FOLLOW_RANGE * 16);
        Path path = navigator.findPath(chunkCache, entity, Set.of(optional.get()), FOLLOW_RANGE, 0, 1F);
        if (path == null) {
            return;
        }
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);
            this.nodes.add(node.asBlockPos().getBottomCenter());
        }
        this.nodes.set(0, pos);
        this.onStart();
        if (this.length() < 1.0) {
            this.noRoad = true;
        }
    }

    @Override
    public Vec3 getCurrentNode() {
        return this.nodes.get(this.currentIndex);
    }

    @Override
    public boolean arrivedAtAnyNode() {
        for (int i = this.currentIndex; i < this.nodes.size(); i++) {
            Vec3 current = this.nodes.get(i);
            Vec3 pos = ServerUtils.getFootPos(this.getFakePlayer());
            if (current.distanceTo(pos) <= 0.5) {
                if (i > 0) {
                    this.setValid();
                }
                this.currentIndex = i + 1;
                if (this.isFinished()) {
                    this.onStop();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean backToBeforeNode() {
        if (this.currentIndex <= 1) {
            return false;
        }
        for (int i = 0; i < this.currentIndex; i++) {
            if (this.nodes.get(i).distanceTo(ServerUtils.getFootPos(this.getFakePlayer())) < 0.5) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFinished() {
        return this.currentIndex >= this.nodes.size();
    }

    @Override
    public void stop() {
        this.currentIndex = this.nodes.size();
        this.onStop();
    }

    @Override
    public void onStart() {
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            EntityPlayerMPFake fakePlayer = this.getFakePlayer();
            MinecraftServer server = ServerUtils.getServer(fakePlayer);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (Loggers.PATHFINDING.isSubscribed(player)) {
                    PlayerUtils.sendNetworkPacket(player, FakePlayerPathfinderS2CPacket.of(fakePlayer.getId(), this.nodes));
                }
            }
        }
    }

    @Override
    public void onStop() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            MinecraftServer server = ServerUtils.getServer(fakePlayer);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (Loggers.PATHFINDING.isSubscribed(player)) {
                    PlayerUtils.sendNetworkPacket(player, FakePlayerPathfinderS2CPacket.of(fakePlayer.getId(), List.of()));
                }
            }
        }
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
        actionPack.setForward(0F);
        actionPack.setSneaking(false);
    }

    @Override
    public boolean isInvalid() {
        if (this.isFinished()) {
            return true;
        }
        Optional<BlockPos> optional = this.target.get();
        if (optional.isEmpty()) {
            return true;
        }
        return this.retryCount > 5;
    }

    /**
     * 将当前路径设置为有效
     */
    private void setValid() {
        this.retryCount = 0;
    }

    @Override
    public boolean isInaccessible() {
        Optional<BlockPos> optional = this.target.get();
        return !optional.map(blockPos -> blockPos.equals(BlockPos.containing(this.nodes.getLast()))).orElse(true);
    }

    private EntityPlayerMPFake getFakePlayer() {
        return this.fakePlayerSupplier.get();
    }

    /**
     * 一个占位实体，没有实际作用
     */
    public static class DummyEntity extends Mob {
        protected DummyEntity(Level world, Vec3 pos) {
            super(EntityTypes.VILLAGER, world);
            this.setPos(pos);
        }
    }
}
