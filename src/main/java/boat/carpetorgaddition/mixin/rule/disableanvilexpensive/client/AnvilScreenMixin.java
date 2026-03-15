package boat.carpetorgaddition.mixin.rule.disableanvilexpensive.client;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
    @ModifyExpressionValue(method = "extractLabels", at = @At(value = "CONSTANT", args = "intValue=40"))
    private int disableExpensive(int original) {
        int value = CarpetOrgAdditionSettings.SET_ANVIL_EXPERIENCE_CONSUMPTION_LIMIT.value();
        return value == -1 ? original : value;
    }
}
