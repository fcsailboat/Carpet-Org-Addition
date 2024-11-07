package org.carpetorgaddition.mixin.rule;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.logger.LoggerNames;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.NetworkPacketLogger;
import org.carpetorgaddition.network.s2c.BeaconBoxUpdateS2CPacket;
import org.carpetorgaddition.util.wheel.BeaconRangeBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
// 大范围信标
public abstract class BeaconBlockEntityMixin {
    @Shadow
    private static int updateLevel(World world, int x, int y, int z) {
        return 0;
    }

    @WrapOperation(method = "applyPlayerEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getNonSpectatingEntities(Ljava/lang/Class;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
    private static List<PlayerEntity> box(World world, Class<PlayerEntity> aClass, Box box, Operation<List<PlayerEntity>> original, @Local(argsOnly = true) BlockPos pos) {
        BeaconRangeBox beaconRangeBox = new BeaconRangeBox(box);
        // 调整信标范围
        if (CarpetOrgAdditionSettings.beaconRangeExpand != 0 && CarpetOrgAdditionSettings.beaconRangeExpand <= 1024) {
            beaconRangeBox = beaconRangeBox.modify(CarpetOrgAdditionSettings.beaconRangeExpand);
        }
        // 调整信标高度
        if (CarpetOrgAdditionSettings.beaconWorldHeight) {
            beaconRangeBox = beaconRangeBox.worldHeight(world);
        }
        // 发送信标范围更新数据包
        if (LoggerRegister.beaconRange) {
            sendBoxUpdate(world, pos, beaconRangeBox);
        }
        return original.call(world, aClass, beaconRangeBox);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 1))
    private static boolean tick(List<?> list, Operation<Boolean> original, @Local(argsOnly = true) BeaconBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getPos();
        if (LoggerRegister.beaconRange && (list.isEmpty() || updateLevel(blockEntity.getWorld(), pos.getX(), pos.getY(), pos.getZ()) == 0)) {
            NetworkPacketLogger logger = (NetworkPacketLogger) LoggerNames.getLogger(LoggerNames.BEACON_RANGE);
            logger.sendPacket(() -> new BeaconBoxUpdateS2CPacket(pos, BeaconBoxUpdateS2CPacket.ZERO));
        }
        return original.call(list);
    }

    @Unique
    @SuppressWarnings("DataFlowIssue")
    private static void sendBoxUpdate(World world, BlockPos pos, BeaconRangeBox beaconRangeBox) {
        int viewDistance = world.getServer().getPlayerManager().getViewDistance();
        for (PlayerEntity player : world.getPlayers()) {
            if (player instanceof EntityPlayerMPFake || pos.toCenterPos().distanceTo(player.getPos()) > viewDistance * 16) {
                continue;
            }
            if (player instanceof ServerPlayerEntity serverPlayerEntity) {
                NetworkPacketLogger logger = (NetworkPacketLogger) LoggerNames.getLogger(LoggerNames.BEACON_RANGE);
                logger.sendPacketIfOnline(serverPlayerEntity, () -> new BeaconBoxUpdateS2CPacket(pos, beaconRangeBox));
            }
        }
    }
}
