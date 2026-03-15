package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SmithingMenu.class)
public abstract class SmithingScreenHandlerMixin {
    @Shadow
    protected abstract List<ItemStack> getRelevantItems();

    // 可重复使用的锻造模板
    @Inject(method = "shrinkStackInSlot", at = @At("HEAD"), cancellable = true)
    private void decrement(int slot, CallbackInfo ci) {
        ItemStack itemStack = this.getRelevantItems().get(slot);
        if (slot == 0) {
            switch (CarpetOrgAdditionSettings.REUSABLE_SMITHING_TEMPLATE.value()) {
                case FALSE -> {
                }
                case UPGRADE -> {
                    if (itemStack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
                        ci.cancel();
                    }
                }
                case TRUE -> ci.cancel();
            }
        }
    }
}