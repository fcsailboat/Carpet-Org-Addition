package boat.carpetorgaddition.periodic.task;

import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class CreeperExplosionTask extends ServerTask {
    // 苦力怕爆炸延迟
    private int countdown = 30;
    private final ServerPlayer player;
    private final Creeper creeper;

    public CreeperExplosionTask(CommandSourceStack source, ServerPlayer player) {
        super(source);
        this.player = player;
        // 传送到玩家周围
        this.creeper = teleport(player);
    }

    // 将苦力怕传送到合适位置
    private static Creeper teleport(ServerPlayer player) {
        Creeper creeper = new Creeper(EntityTypes.CREEPER, ServerUtils.getWorld(player));
        BlockPos playerPos = player.blockPosition();
        Vec3 fromPos = new Vec3(playerPos.getX() - 3, playerPos.getY() - 1, playerPos.getZ() - 3);
        Vec3 toPos = new Vec3(playerPos.getX() + 3, playerPos.getY() + 1, playerPos.getZ() + 3);
        ArrayList<BlockPos> list = new ArrayList<>();
        Level world = ServerUtils.getWorld(player);
        // 获取符合条件的坐标
        for (BlockPos blockPos : new BlockPosTraverser(new AABB(fromPos, toPos))) {
            // 当前方块是空气
            if (world.getBlockState(blockPos).isAir()
                // 下方方块是实心方块
                && world.getBlockState(blockPos.below()).isRedstoneConductor(world, blockPos.below())
                // 上方方块是空气
                && world.getBlockState(blockPos.above()).isAir()) {
                list.add(blockPos);
            }
        }
        // 将苦力怕传送到随机坐标
        BlockPos randomPos = list.isEmpty() ? playerPos : list.get(MathUtils.randomInt(1, list.size()) - 1);
        TeleportTransition target = new TeleportTransition(ServerUtils.getWorld(player), randomPos.getBottomCenter(), Vec3.ZERO, 0F, 0F, TeleportTransition.DO_NOTHING);
        return (Creeper) creeper.teleport(target);
    }

    @Override
    public void tick() {
        if (this.countdown == 30) {
            // 播放爆炸引线音效
            this.creeper.playSound(SoundEvents.CREEPER_PRIMED, 1.0f, 0.5f);
            this.creeper.gameEvent(GameEvent.PRIME_FUSE);
        }
        this.countdown--;
        if (this.countdown == 0) {
            // 产生爆炸
            ServerUtils.getWorld(this.player).explode(creeper, this.creeper.getX(), this.player.getY(),
                    this.player.getZ(), 3F, false, Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    public boolean stopped() {
        // 苦力怕倒计时结束或苦力怕距离玩家超过7格
        if (this.countdown < 0 || this.player.distanceTo(this.creeper) > 7) {
            this.creeper.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            return this.player.equals(((CreeperExplosionTask) obj).player);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.player.hashCode();
    }
}
