package boat.carpetorgaddition.mixin.render.client;

import boat.carpetorgaddition.client.render.waypoint.WaypointRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void render(DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix, @Local(name = "projectionMatrix") Matrix4f projectionMatrix) {
        WaypointRenderer.getInstance().setModelView(modelViewMatrix);
        WaypointRenderer.getInstance().setProjection(projectionMatrix);
    }
}
