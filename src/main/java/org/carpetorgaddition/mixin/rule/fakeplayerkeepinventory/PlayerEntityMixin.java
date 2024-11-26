package org.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Unique
    private final PlayerEntity thisPlayer = (PlayerEntity) (Object) this;

    // 假玩家死亡不掉落，保留物品栏
    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void dropInventory(CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.fakePlayerKeepInventory && thisPlayer instanceof EntityPlayerMPFake) {
            ci.cancel();
        }
    }

    // 假玩家死亡不掉落经验
    @Inject(method = "getExperienceToDrop", at = @At("HEAD"), cancellable = true)
    private void getXpToDrop(CallbackInfoReturnable<Integer> cir) {
        if (CarpetOrgAdditionSettings.fakePlayerKeepInventory && thisPlayer instanceof EntityPlayerMPFake) {
            cir.setReturnValue(0);
        }
    }
}
