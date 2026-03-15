package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 激流忽略天气
@Mixin(TridentItem.class)
public class TridentItemMixin {
    @WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean isTouchingWaterOrRain(Player instance, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.RIPTIDE_IGNORE_CONDITIONS.value()) {
            return true;
        }
        return original.call(instance);
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean useIsTouchingWaterOrRain(Player instance, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.RIPTIDE_IGNORE_CONDITIONS.value()) {
            return true;
        }
        return original.call(instance);
    }
}
