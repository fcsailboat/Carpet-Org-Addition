package boat.carpetorgaddition.mixin.render.client;

import boat.carpetorgaddition.client.render.waypoint.WaypointRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void render(CallbackInfo ci, @Local(name = "projectionMatrix") Matrix4f projectionMatrix) {
        WaypointRenderer.getInstance().setProjection(projectionMatrix);
    }
}
