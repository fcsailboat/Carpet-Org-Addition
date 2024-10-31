package org.carpetorgaddition.mixin.util;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    // 隐藏玩家登录登出的消息
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void broadcast(Text message, boolean overlay, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.hiddenLoginMessages) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "onPlayerConnect", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V"))
    private boolean hide(Logger instance, String s, Object[] objects) {
        return !CarpetOrgAdditionSettings.hiddenLoginMessages;
    }
}
