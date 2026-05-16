package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ResolvableProfile.Dynamic.class)
public class ResolvableProfileDynamicMixin {
    @WrapOperation(method = "resolveProfile", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<GameProfile> async(Supplier<GameProfile> supplier, Executor executor, Operation<CompletableFuture<GameProfile>> original) {
        boolean request = BatchSpawnFakePlayerTask.REQUEST.orElse(false);
        Supplier<GameProfile> wrapper = () -> ScopedValue.where(BatchSpawnFakePlayerTask.REQUEST, request).call(supplier::get);
        return original.call(wrapper, executor);
    }
}
