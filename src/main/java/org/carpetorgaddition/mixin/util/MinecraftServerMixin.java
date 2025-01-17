package org.carpetorgaddition.mixin.util;

import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.periodic.PeriodicTaskManagerInterface;
import org.carpetorgaddition.periodic.ServerPeriodicTaskManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements PeriodicTaskManagerInterface {
    @Unique
    private ServerPeriodicTaskManager manager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        // 在构造方法执行完毕后创建，因为在这之前服务器可能没有完成初始化
        this.manager = new ServerPeriodicTaskManager((MinecraftServer) (Object) this);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        this.manager.tick();
    }

    @Override
    public ServerPeriodicTaskManager carpet_Org_Addition$getServerPeriodicTaskManager() {
        return Objects.requireNonNull(this.manager);
    }
}
