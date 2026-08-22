package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.ColorValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PlaneRenderComponent implements WorldRenderComponent {
    private final Vec3 vertex1;
    private final Vec3 vertex2;
    private final Vec3 vertex3;
    private final Vec3 vertex4;
    private final ColorValue color = ColorValue.of();

    public PlaneRenderComponent(Vec3 vertex1, Vec3 vertex2, Vec3 vertex3, Vec3 vertex4) {
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
        Vec3 cameraPos = camera.position();
        SubmitNodeCollector collector = context.submitNodeCollector();
        collector.submitCustomGeometry(stack, RenderTypes.debugQuads(), (pose, buffer) -> {
            // 平面顶点向摄像机靠近的偏移量，用于消除深度冲突
            final float offset = 0.0001F;
            List.of(this.vertex1, this.vertex2, this.vertex3, this.vertex4)
                    .forEach(vertex -> buffer.addVertex(pose, MathUtils.move(vertex, cameraPos, offset).toVector3f())
                            .setColor(
                                    this.color.getRed(),
                                    this.color.getGreen(),
                                    this.color.getBlue(),
                                    this.color.getAlpha()
                            ));
        });
        stack.popPose();
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    public void setRgba(int rgba) {
        this.color.setRgba(rgba);
    }
}
