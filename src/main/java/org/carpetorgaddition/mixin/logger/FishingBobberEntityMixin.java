package org.carpetorgaddition.mixin.logger;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.logger.FunctionLogger;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.Loggers;
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
            FunctionLogger logger = Loggers.getFishingLogger();
            if (this.getPlayerOwner() instanceof ServerPlayerEntity player && logger.isSubscribed(player)) {
                if (this.waitCountdown > 0) {
                    // 鱼出现
                    MessageUtils.sendMessageToHud(player, TextUtils.translate("carpet.logger.fishing.appear", this.waitCountdown));
                } else if (this.fishTravelCountdown > 0) {
                    // 鱼上钩
                    MessageUtils.sendMessageToHud(player, TextUtils.translate("carpet.logger.fishing.bite", this.fishTravelCountdown));
                } else if (this.hookCountdown > 0) {
                    // 鱼挣脱
                    MutableText translate = TextUtils.translate("carpet.logger.fishing.break_free", this.hookCountdown);
                    MessageUtils.sendMessageToHud(player, TextUtils.setColor(translate, Formatting.GREEN));
                }
            }
        }
    }
}
