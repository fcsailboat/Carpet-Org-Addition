package org.carpetorgaddition.mixin.debug;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.spawner.PhantomSpawner;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyDeveloped
@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
    @Shadow
    private int cooldown;

    @Inject(method = "spawn", at = @At("HEAD"))
    private void spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals, CallbackInfoReturnable<Integer> cir) {
        if (DebugSettings.phantomImmediatelySpawn) {
            this.cooldown = 0;
        }
    }
}
