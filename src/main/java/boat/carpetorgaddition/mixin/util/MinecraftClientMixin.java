package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @WrapOperation(method = {"readInputStreamWithEtag", "buildHttpException"}, at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void suppressionWarning(Logger instance, String message, Object url, Object result, Operation<Void> original) {
        if (BatchSpawnFakePlayerTask.REQUEST.orElse(false)) {
            return;
        }
        original.call(instance, message, url, result);
    }
}
