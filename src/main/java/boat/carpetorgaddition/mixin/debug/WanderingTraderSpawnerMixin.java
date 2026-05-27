package boat.carpetorgaddition.mixin.debug;

import boat.carpetorgaddition.command.RuntimeCommand;
import boat.carpetorgaddition.debug.OnlyDeveloped;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyDeveloped
@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderSpawnerMixin {
    @WrapOperation(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int spawn(RandomSource instance, int i, Operation<Integer> original) {
        return RuntimeCommand.SPAWN_WANDERING_TRADERS.orElse(false) ? 0 : original.call(instance, i);
    }
}
