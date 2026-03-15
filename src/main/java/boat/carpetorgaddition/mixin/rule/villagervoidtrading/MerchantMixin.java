package boat.carpetorgaddition.mixin.rule.villagervoidtrading;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Merchant.class)
public interface MerchantMixin {
    @Shadow
    MerchantOffers getOffers();

    @Inject(method = "openTradingScreen", at = @At("HEAD"))
    private void restock(Player player, Component title, int level, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.VILLAGER_INFINITE_TRADE.value()) {
            this.getOffers().forEach(MerchantOffer::resetUses);
        }
    }
}
