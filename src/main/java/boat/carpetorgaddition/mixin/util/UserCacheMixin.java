package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.wheel.GameProfileCache;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CachedUserNameToIdResolver.class)
public class UserCacheMixin {
    @Inject(method = "add(Lnet/minecraft/server/players/NameAndId;)V", at = @At("HEAD"))
    private void add(NameAndId nameAndId, CallbackInfo ci) {
        GameProfileCache.getInstance().put(nameAndId);
    }
}
