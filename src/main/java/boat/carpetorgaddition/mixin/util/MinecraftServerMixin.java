package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.PeriodicTaskManagerInterface;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import net.minecraft.server.MinecraftServer;
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
    private final MinecraftServer self = (MinecraftServer) (Object) this;
    @Unique
    private ServerComponentCoordinator manager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        // 在构造方法执行完毕后创建，因为在这之前服务器可能没有完成初始化
        this.manager = ScopedValue.where(ServerComponentCoordinator.SERVER_INSTANCE, this.self)
                .call(() -> new ServerComponentCoordinator(this.self));
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void tick(BooleanSupplier haveTime, CallbackInfo ci) {
        this.manager.tick();
    }

    @Override
    public ServerComponentCoordinator carpet_Org_Addition$getServerPeriodicTaskManager() {
        return Objects.requireNonNull(this.manager);
    }
}
