package org.carpetorgaddition.mixin.command;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.damage.DamageSource;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSafeAfkInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMPFake.class)
public abstract class EntityPlayerMPFakeMixin {
    @Unique
    private boolean isDead = false;

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource cause, CallbackInfo ci) {
        this.isDead = true;
    }

    /**
     * @apiNote 尽管这个方法没有 {@code @Override} 注解，但这这不妨碍它重写了接口{@link FakePlayerSafeAfkInterface}中的
     * {@code afkTriggerFail()}方法，该接口在父类的Mixin类中被实现
     */
    @SuppressWarnings({"MissingUnique", "unused"})
    public boolean afkTriggerFail() {
        return isDead;
    }
}
