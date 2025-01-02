package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.periodic.task.ServerTaskManagerInterface;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.ArrayList;

public class CreeperCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("creeper")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandCreeper))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(CreeperCommand::creeperExplosion)));
    }

    // 创建苦力怕并爆炸
    private static int creeperExplosion(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = CommandUtils.getArgumentPlayer(context);
        ServerTaskManagerInterface anInterface = ServerTaskManagerInterface.getInstance(context.getSource().getServer());
        // 添加苦力怕爆炸任务
        anInterface.addTask(new CreeperExplosionTask(targetPlayer));
        ServerPlayerEntity sourcePlayer = context.getSource().getPlayer();
        if (sourcePlayer != null) {
            CarpetOrgAddition.LOGGER.info("{}在{}周围制造了一场苦力怕爆炸",
                    sourcePlayer.getName().getString(), targetPlayer.getName().getString());
        }
        return 1;
    }

    private static class CreeperExplosionTask extends ServerTask {
        // 苦力怕爆炸延迟
        private int countdown = 30;
        private final ServerPlayerEntity player;
        private final CreeperEntity creeper;

        private CreeperExplosionTask(ServerPlayerEntity player) {
            this.player = player;
            // 传送到玩家周围
            this.creeper = teleport(player);
        }

        // 将苦力怕传送到合适位置
        private static CreeperEntity teleport(ServerPlayerEntity player) {
            CreeperEntity creeper = new CreeperEntity(EntityType.CREEPER, player.getWorld());
            BlockPos playerPos = player.getBlockPos();
            Vec3d fromPos = new Vec3d(playerPos.getX() - 3, playerPos.getY() - 1, playerPos.getZ() - 3);
            Vec3d toPos = new Vec3d(playerPos.getX() + 3, playerPos.getY() + 1, playerPos.getZ() + 3);
            SelectionArea selectionArea = new SelectionArea(new Box(fromPos, toPos));
            ArrayList<BlockPos> list = new ArrayList<>();
            World world = player.getWorld();
            // 获取符合条件的坐标
            for (BlockPos blockPos : selectionArea) {
                // 当前方块是空气
                if (world.getBlockState(blockPos).isAir()
                        // 下方方块是实心方块
                        && world.getBlockState(blockPos.down()).isSolidBlock(world, blockPos.down())
                        // 上方方块是空气
                        && world.getBlockState(blockPos.up()).isAir()) {
                    list.add(blockPos);
                }
            }
            // 将苦力怕传送到随机坐标
            BlockPos randomPos = list.isEmpty() ? playerPos : list.get(MathUtils.randomInt(1, list.size()) - 1);
            TeleportTarget target = new TeleportTarget(player.getServerWorld(), randomPos.toBottomCenterPos(), Vec3d.ZERO, 0F, 0F, TeleportTarget.NO_OP);
            return (CreeperEntity) creeper.teleportTo(target);
        }

        @Override
        public void tick() {
            if (this.countdown == 30) {
                // 播放爆炸引线音效
                this.creeper.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);
                this.creeper.emitGameEvent(GameEvent.PRIME_FUSE);
            }
            this.countdown--;
            if (this.countdown == 0) {
                // 产生爆炸
                this.player.getWorld().createExplosion(creeper, this.creeper.getX(), this.player.getY(),
                        this.player.getZ(), 3F, false, World.ExplosionSourceType.NONE);
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
        public String getLogName() {
            return "苦力怕爆炸";
        }
    }
}
