package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Vector3f;

public class PlaneRenderComponent implements WorldRenderComponent {
    private final Vector3f vertex1;
    private final Vector3f vertex2;
    private final Vector3f vertex3;
    private final Vector3f vertex4;

    public PlaneRenderComponent(Vector3f vertex1, Vector3f vertex2, Vector3f vertex3, Vector3f vertex4) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.vertex3 = vertex3;
        this.vertex4 = vertex4;
    }

    public PlaneRenderComponent(ShapePlane shapePlane) {
        this(shapePlane.vertex1(), shapePlane.vertex2(), shapePlane.vertex3(), shapePlane.vertex4());
    }

    @Override
    public void render(LevelRenderContext context) {
        PoseStack stack = context.poseStack();
        stack.pushPose();
        Camera camera = ClientUtils.getCamera();
        Vector3f cameraPos = camera.position().toVector3f();
        SubmitNodeCollector collector = context.submitNodeCollector();
        collector.submitCustomGeometry(stack, RenderTypes.debugQuads(), (pose, buffer) -> {
            // 平面顶点向摄像机靠近的偏移量，用于消除深度冲突
            final float offset = 0.0001F;
            buffer.addVertex(pose, MathUtils.approach(this.vertex1, cameraPos, offset)).setColor(-1);
            buffer.addVertex(pose, MathUtils.approach(this.vertex2, cameraPos, offset)).setColor(-1);
            buffer.addVertex(pose, MathUtils.approach(this.vertex3, cameraPos, offset)).setColor(-1);
            buffer.addVertex(pose, MathUtils.approach(this.vertex4, cameraPos, offset)).setColor(-1);
        });
        stack.popPose();
    }

    @Override
    public boolean isStopped() {
        return false;
    }
}
