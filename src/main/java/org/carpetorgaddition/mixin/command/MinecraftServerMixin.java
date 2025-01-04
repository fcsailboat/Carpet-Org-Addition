package org.carpetorgaddition.mixin.command;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

/*@SuppressWarnings("AddedMixinMembersNamePattern")*/
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin /*implements ExpressManagerInterface */ {
/*    @Shadow
    public abstract void tick(BooleanSupplier shouldKeepTicking);

    @Unique
    private final MinecraftServer thisServer = (MinecraftServer) (Object) this;

    *
    @Unique
    private ExpressManager expressManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        expressManager = new ExpressManager(thisServer);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        this.expressManager.tick();
    }

    @Override
    public ExpressManager getExpressManager() {
        return this.expressManager;
    }*/
}
