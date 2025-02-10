package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.spawner.PhantomSpawner;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
    // 限制幻翼生成
    @Inject(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SpawnHelper;isClearForSpawn(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;Lnet/minecraft/entity/EntityType;)Z"), cancellable = true)
    private void spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals, CallbackInfo ci, @Local(ordinal = 1) BlockPos blockPos) {
        if (CarpetOrgAdditionSettings.limitPhantomSpawn) {
            SpawnHelper.Info spawnInfo = world.getChunkManager().getSpawnInfo();
            if (spawnInfo == null) {
                return;
            }
            SpawnHelperInfoAccessor accessor = (SpawnHelperInfoAccessor) spawnInfo;
            boolean isBelowCap = accessor.invokerIsBelowCap(EntityType.PHANTOM.getSpawnGroup());
            boolean canSpawn = accessor.invokerCanSpawn(EntityType.PHANTOM.getSpawnGroup(), new ChunkPos(blockPos));
            if (isBelowCap && canSpawn) {
                return;
            }
            ci.cancel();
        }
    }
}
