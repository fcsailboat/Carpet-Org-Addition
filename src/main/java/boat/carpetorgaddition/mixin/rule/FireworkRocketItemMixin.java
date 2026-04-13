package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {
    @Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void useOnBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null) {
            return;
        }
        //烟花火箭使用冷却(对方块使用)
        if (CarpetOrgAdditionSettings.FIREWORK_ROCKET_USE_COOLDOWN.value()) {
            player.getCooldowns().addCooldown(context.getItemInHand(), 5);
        }
    }

    //烟花火箭使用冷却(使用鞘翅飞行时)
    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    private void use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (CarpetOrgAdditionSettings.FIREWORK_ROCKET_USE_COOLDOWN.value() && player != null && player.isFallFlying()) {
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 5);
        }
    }
}