package boat.carpetorgaddition.mixin.logger;

import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingBobberEntityMixin {
    @Shadow
    @Nullable
    public abstract Player getPlayerOwner();

    @Shadow
    private int nibble;

    @Shadow
    private int timeUntilLured;

    @Shadow
    private int timeUntilHooked;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (Loggers.FISHING.isEnable()) {
            if (this.getPlayerOwner() instanceof ServerPlayer player && Loggers.FISHING.isSubscribed(player)) {
                LocalizationKey key = Loggers.FISHING.getLocalizationKey();
                if (this.timeUntilLured > 0) {
                    // 鱼出现
                    MessageUtils.sendMessageToHud(player, key.then("appear").translate(this.timeUntilLured));
                } else if (this.timeUntilHooked > 0) {
                    // 鱼上钩
                    MessageUtils.sendMessageToHud(player, key.then("bite").translate(this.timeUntilHooked));
                } else if (this.nibble > 0) {
                    // 鱼挣脱
                    TextBuilder builder = TextBuilder.of(key.then("break_free").translate(this.nibble));
                    builder.setColor(ChatFormatting.GREEN);
                    MessageUtils.sendMessageToHud(player, builder.build());
                }
            }
        }
    }
}
