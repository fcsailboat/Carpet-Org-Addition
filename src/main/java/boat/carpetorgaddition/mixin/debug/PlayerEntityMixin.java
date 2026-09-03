package boat.carpetorgaddition.mixin.debug;

import boat.carpetorgaddition.debug.DebugSettings;
import boat.carpetorgaddition.debug.OnlyDeveloped;
import boat.carpetorgaddition.exception.ProductionEnvironmentError;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.wheel.common.CommonCommands;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyDeveloped
@Mixin(value = Player.class, priority = 1001)
public class PlayerEntityMixin {
    @Unique
    private final Player thisPlayer = (Player) (Object) this;

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void openInventory(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand == InteractionHand.OFF_HAND) {
            return;
        }
        ProductionEnvironmentError.assertDevelopmentEnvironment();
        if (DebugSettings.openFakePlayerInventory.get() && entity instanceof EntityPlayerMPFake fakePlayer) {
            if (thisPlayer instanceof ServerPlayer player) {
                CommandUtils.execute(player, CommonCommands.openPlayerInventory(fakePlayer));
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
