package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {
    @WrapOperation(method = "handleBlockBreakAction", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void suppressionLogOut(Logger instance, String s, Object o1, Object o2, Operation<Void> original) {
        // 抑制方块破坏位置不匹配输出
        if (CarpetOrgAdditionSettings.SUPPRESSION_MISMATCH_IN_DESTROY_BLOCK_POS_WARN.value()) {
            return;
        }
        original.call(instance, s, o1, o2);
    }
}
