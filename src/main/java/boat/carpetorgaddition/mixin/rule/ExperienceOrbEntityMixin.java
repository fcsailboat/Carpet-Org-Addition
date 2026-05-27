package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbEntityMixin {
    @Shadow
    private int age;

    @Shadow
    private int count;

    @Shadow
    public abstract int getValue();

    @Shadow
    protected abstract void setValue(int value);

    @Unique
    private final ExperienceOrb thisEntity = (ExperienceOrb) (Object) this;

    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;II)Z", at = @At("HEAD"), cancellable = true)
    private static void isMergeable(ExperienceOrb orb, int id, int value, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.EXPERIENCE_ORB_MERGE.value()) {
            boolean combine = ((ExperienceOrbEntityMixin) (Object) orb).combine();
            if (combine) {
                cir.setReturnValue(!orb.isRemoved() && orb.getValue() + value <= Short.MAX_VALUE);
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "merge", at = @At("HEAD"), cancellable = true)
    private void merge(ExperienceOrb orb, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.EXPERIENCE_ORB_MERGE.value() && this.combine()) {
            int sum = this.getValue() * this.count + orb.getValue() * ((ExperienceOrbEntityMixin) (Object) orb).count;
            if (sum > Short.MAX_VALUE) {
                ci.cancel();
                return;
            }
            this.setValue(sum);
            this.age = Math.min(this.age, ((ExperienceOrbEntityMixin) (Object) orb).age);
            this.count = 1;
            orb.discard();
            ci.cancel();
        }
    }

    /**
     * 此方案可以提高玩家吸收经验球的效率，但是经验球过多时合并的效率不如原版。因此每隔一段时间切换一次合并方式，两种方式结合起来使用。
     */
    @Unique
    private boolean combine() {
        long time = ServerUtils.getWorld(thisEntity).getGameTime();
        return Math.sin(time / 10.0) > 0;
    }
}
