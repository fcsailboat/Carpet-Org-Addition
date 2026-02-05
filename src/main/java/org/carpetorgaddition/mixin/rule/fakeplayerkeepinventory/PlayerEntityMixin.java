package org.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Unique
    private final PlayerEntity thisPlayer = (PlayerEntity) (Object) this;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique
    private boolean shouldKeepInventory() {
        if (CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() && thisPlayer instanceof EntityPlayerMPFake fakePlayer) {
            return RuleUtils.shouldKeepInventory(fakePlayer);
        }
        return false;
    }

    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void dropInventory(CallbackInfo ci) {
        if (shouldKeepInventory()) {
            ci.cancel();
        }
    }

    // 假玩家死亡不掉落经验
    @Inject(method = "getExperienceToDrop", at = @At("HEAD"), cancellable = true)
    private void getXpToDrop(CallbackInfoReturnable<Integer> cir) {
        if (shouldKeepInventory()) {
            cir.setReturnValue(0);
        }
    }
}
