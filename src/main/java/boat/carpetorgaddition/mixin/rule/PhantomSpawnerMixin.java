package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.mixin.accessor.SpawnHelperInfoAccessor;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
    // 限制幻翼生成
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;isValidEmptySpawnBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/entity/EntityType;)Z"), cancellable = true)
    private void spawn(ServerLevel world, boolean spawnMonsters, CallbackInfo ci, @Local(name = "spawnPos") BlockPos blockPos) {
        if (CarpetOrgAdditionSettings.LIMIT_PHANTOM_SPAWN.value()) {
            NaturalSpawner.SpawnState spawnInfo = world.getChunkSource().getLastSpawnState();
            if (spawnInfo == null) {
                return;
            }
            SpawnHelperInfoAccessor accessor = (SpawnHelperInfoAccessor) spawnInfo;
            boolean isBelowCap = accessor.invokerIsBelowCap(EntityTypes.PHANTOM.getCategory());
            boolean canSpawn = accessor.invokerCanSpawn(EntityTypes.PHANTOM.getCategory(), ChunkPos.containing(blockPos));
            if (isBelowCap && canSpawn) {
                return;
            }
            ci.cancel();
        }
    }
}
