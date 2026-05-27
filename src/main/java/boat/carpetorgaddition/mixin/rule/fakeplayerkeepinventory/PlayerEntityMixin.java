package boat.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Unique
    private final Player thisPlayer = (Player) (Object) this;

    @SuppressWarnings("unused")
    private PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Unique
    private boolean shouldKeepInventory() {
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_KEEP_INVENTORY.value() && thisPlayer instanceof EntityPlayerMPFake fakePlayer) {
            return RuleUtils.shouldKeepInventory(fakePlayer);
        }
        return false;
    }

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void dropInventory(CallbackInfo ci) {
        if (shouldKeepInventory()) {
            ci.cancel();
        }
    }

    // 假玩家死亡不掉落经验
    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void getXpToDrop(CallbackInfoReturnable<Integer> cir) {
        if (shouldKeepInventory()) {
            cir.setReturnValue(0);
        }
    }
}
