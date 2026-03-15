package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.screen.VillagerScreenHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 村民立即补货
@Mixin(Villager.class)
public abstract class VillagerEntityMixin extends AbstractVillager {
    @Unique
    private final Villager thisVillager = (Villager) (Object) this;

    public VillagerEntityMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    // 打开村民物品栏
    @Inject(method = "mobInteract", at = @At(value = "HEAD"), cancellable = true)
    private void clearVillagerInventory(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (CarpetOrgAdditionSettings.OPEN_VILLAGER_INVENTORY.value() && player.isShiftKeyDown()) {
            PlayerUtils.openScreenHandler(
                    player,
                    (syncId, inventory, _) -> new VillagerScreenHandler(syncId, inventory, thisVillager),
                    thisVillager.getName()
            );
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    // 村民回血
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void heal(CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.VILLAGER_HEAL.value()) {
            long worldTime = ServerUtils.getWorld(thisVillager).getGameTime();
            // 每四秒回一次血
            if (worldTime % 80 == 0) {
                thisVillager.heal(1.0F);
            }
        }
    }
}
