package org.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.rule.value.FakePlayerKeepInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow public int totalExperience;
    @Unique
    private final PlayerEntity thisPlayer = (PlayerEntity) (Object) this;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }



    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void CandropInventory(CallbackInfo ci) {
        if(CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.TRUE && thisPlayer instanceof EntityPlayerMPFake fakePlayer) {
            if(RuleUtils.shouldKeepInventory(fakePlayer)){
                ci.cancel();
            }else{
                ExecutionerInventory(fakePlayer);
                ci.cancel();
            }
        } else if (CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.FALSE && thisPlayer instanceof EntityPlayerMPFake fakePlayer){
            if(RuleUtils.shouldKeepInventory(fakePlayer)){
                ci.cancel();
            }else{
            ExecutionerInventory(fakePlayer);
            ci.cancel();
            }
        }
    }
    @Unique
    private static void ExecutionerInventory(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                player.dropItem(stack, true, false);
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }
    @Inject(method = "getXpToDrop", at = @At("HEAD"), cancellable = true)
    private void getXpToDrop(CallbackInfoReturnable<Integer> cir) {
        if(CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.TRUE && thisPlayer instanceof EntityPlayerMPFake fakePlayer) {
            if(RuleUtils.shouldKeepInventory(fakePlayer)){
                cir.setReturnValue(0);
            }else{
                int i = thisPlayer.experienceLevel * 7;
                int xp = i > 100 ? 100 : i;
                cir.setReturnValue(xp);
            }
        } else if (CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.FALSE&&thisPlayer instanceof EntityPlayerMPFake fakePlayer){
            if(RuleUtils.shouldKeepInventory(fakePlayer)){
                cir.setReturnValue(0);
            }else{
                int i = thisPlayer.experienceLevel * 7;
                int xp = i > 100 ? 100 : i;
                cir.setReturnValue(xp);
            }
        }
    }
}
