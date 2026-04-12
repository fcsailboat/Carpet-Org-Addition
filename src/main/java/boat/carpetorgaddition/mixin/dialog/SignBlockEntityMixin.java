package boat.carpetorgaddition.mixin.dialog;

import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.network.event.CustomClickActionContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin {
    @WrapOperation(method = "executeClickCommandsIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"))
    private void setPlayer(MinecraftServer instance, Identifier id, Optional<Tag> payload, Operation<Void> original, @Local(argsOnly = true, name = "player") Player player) {
        if (player instanceof ServerPlayer) {
            ScopedValue.where(CustomClickActionContext.CURRENT_PLAYER, (ServerPlayer) player)
                    .run(() -> original.call(instance, id, payload));
        } else {
            original.call(instance, id, payload);
        }
    }

    @WrapOperation(method = "executeClickCommandsIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"))
    private void setActionSource(MinecraftServer instance, Identifier id, Optional<Tag> payload, Operation<Void> original) {
        ScopedValue.where(CustomClickActionContext.ACTION_SOURCE, ActionSource.SIGN)
                .run(() -> original.call(instance, id, payload));
    }
}
