package boat.carpetorgaddition.mixin.dialog;

import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.network.event.CustomClickActionContext;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Unique
    private final MinecraftServer self = (MinecraftServer) (Object) this;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(method = "handleCustomClickAction", at = @At("HEAD"))
    private void handleCustomClickAction(Identifier identifier, Optional<Tag> optional, CallbackInfo ci) {
        if (CustomClickActionContext.CURRENT_PLAYER.isBound()) {
            ServerPlayer player = CustomClickActionContext.CURRENT_PLAYER.get();
            CustomClickActionContext context = new CustomClickActionContext(self, player, optional.orElse(null));
            CustomClickAction.accept(identifier, context);
        }
    }
}
