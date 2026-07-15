package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.task.search.AbstractOfflinePlayerSearchTask;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleCriterionTrigger.class)
public class SimpleCriterionTriggerMixin<T extends SimpleCriterionTrigger.SimpleInstance> {
    @Inject(method = "addPlayerListener", at = @At("HEAD"), cancellable = true)
    private void addPlayerListener(PlayerAdvancements player, CriterionTrigger.Listener<T> listener, CallbackInfo ci) {
        if (AbstractOfflinePlayerSearchTask.UPGRADING.orElse(false) && player.player instanceof FakePlayer) {
            ci.cancel();
        }
    }
}
