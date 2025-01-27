package org.carpetorgaddition.mixin.logger;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.logger.FunctionLogger;
import org.carpetorgaddition.logger.LoggerNames;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @Shadow
    @Nullable
    public abstract PlayerEntity getPlayerOwner();

    @Shadow
    private int hookCountdown;

    @Shadow
    private int waitCountdown;

    @Shadow
    private int fishTravelCountdown;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (LoggerRegister.fishing) {
            PlayerEntity player = this.getPlayerOwner();
            FunctionLogger logger = (FunctionLogger) LoggerNames.getLogger(LoggerNames.FISHING);
            if (player instanceof ServerPlayerEntity serverPlayer && logger.isSubscribed(serverPlayer)) {
                // TODO 值可能为-1，不能使用!=0判断
                // TODO 只显示与出现倒计时，改为距离鱼上钩还有<出现时间>+<上钩时间>刻
                if (this.waitCountdown != 0) {
                    // 鱼出现
                    MessageUtils.sendMessageToHud(serverPlayer, TextUtils.translate("carpet.logger.fishing.appear", this.waitCountdown));
                } else if (this.fishTravelCountdown != 0) {
                    // 鱼上钩
                    MessageUtils.sendMessageToHud(serverPlayer, TextUtils.translate("carpet.logger.fishing.bite", this.fishTravelCountdown));
                } else if (this.hookCountdown != 0) {
                    // 鱼挣脱
                    MutableText translate = TextUtils.translate("carpet.logger.fishing.break_free", this.hookCountdown);
                    MessageUtils.sendMessageToHud(serverPlayer, TextUtils.setColor(translate, Formatting.GREEN));
                }
            }
        }
    }
}
