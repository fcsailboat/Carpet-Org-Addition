package org.carpetorgaddition.mixin.debug;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.util.CommandUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyDeveloped
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void openInventory(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ProductionEnvironmentError.assertDevelopmentEnvironment();
        if (DebugSettings.openFakePlayerInventory && entity instanceof EntityPlayerMPFake fakePlayer) {
            ServerCommandSource source = ((PlayerEntity) (Object) this).getCommandSource(fakePlayer.getServerWorld());
            CommandUtils.execute(source, "playerTools %s inventory".formatted(fakePlayer.getName().getString()));
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
