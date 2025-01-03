package org.carpetorgaddition.mixin.command;

import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.express.ExpressManagerInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ExpressManagerInterface {
    @Shadow
    public abstract void tick(BooleanSupplier shouldKeepTicking);

    @Unique
    private final MinecraftServer thisServer = (MinecraftServer) (Object) this;

    /**
     * 快递管理器
     */
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

    /**
     * @return 快递管理器
     */
    @Override
    public ExpressManager getExpressManager() {
        return this.expressManager;
    }
}
