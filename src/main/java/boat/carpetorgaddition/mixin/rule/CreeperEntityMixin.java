package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
public abstract class CreeperEntityMixin {
    @Shadow
    private boolean droppedSkulls;

    @Shadow
    public abstract boolean isPowered();

    //和平的苦力怕
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void setTarget(LivingEntity target, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.PEACEFUL_CREEPER.value() && target instanceof Player) {
            ci.cancel();
        }
    }

    @Inject(method = "killedEntity", at = @At("HEAD"))
    private void onHeadDropped(ServerLevel world, LivingEntity other, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        // 闪电苦力怕同时炸死多个生物时每个都掉落头颅
        if (CarpetOrgAdditionSettings.SUPER_CHARGED_CREEPER.value()) {
            this.droppedSkulls = false;
        }
        // 玩家被闪电苦力怕炸死掉落头颅
        if (CarpetOrgAdditionSettings.PLAYER_DROP_HEAD.value() && this.isPowered() && !this.droppedSkulls && other instanceof ServerPlayer player) {
            ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
            itemStack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
            player.spawnAtLocation(player.level(), itemStack);
        }
    }
}