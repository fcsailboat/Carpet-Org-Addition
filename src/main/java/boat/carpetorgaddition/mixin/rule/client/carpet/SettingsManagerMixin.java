package boat.carpetorgaddition.mixin.rule.client.carpet;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.client.util.ClientUtils;
import carpet.api.settings.SettingsManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 该Mixin类仅在客户端加载
@Mixin(SettingsManager.class)
public class SettingsManagerMixin {
    // 开放/carpet命令权限，仅单人游戏
    @Inject(method = "lambda$registerCommand$1", at = @At("HEAD"), cancellable = true)
    private void carpet(CommandSourceStack player, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.OPEN_CARPET_PERMISSION.value()) {
            IntegratedServer clientServer = ClientUtils.getServer();
            if (clientServer != null) {
                cir.setReturnValue(true);
            }
        }
    }
}
