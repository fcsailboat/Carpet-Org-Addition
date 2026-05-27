package boat.carpetorgaddition.mixin.debug;

import boat.carpetorgaddition.debug.DebugSettings;
import boat.carpetorgaddition.debug.OnlyDeveloped;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyDeveloped
@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
    @SuppressWarnings("unused")
    @Shadow
    private int nextTick;

    @Inject(method = "tick", at = @At("HEAD"))
    private void spawn(ServerLevel level, boolean spawnEnemies, CallbackInfo ci) {
        if (DebugSettings.phantomImmediatelySpawn.get()) {
            this.nextTick = 0;
        }
    }
}
