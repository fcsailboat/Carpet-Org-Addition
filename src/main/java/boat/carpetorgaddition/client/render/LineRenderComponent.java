package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class LineRenderComponent implements WorldRenderComponent, RgbaSettable, WidthSettable {
    private final Vec3 from;
    private final Vec3 to;
    private int red = 0xFF;
    private int green = 0xFF;
    private int blue = 0xFF;
    private int alpha = 0xFF;
    private float width = 1F;

    public LineRenderComponent(Vec3 from, Vec3 to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void render(LevelRenderContext context) {
        PoseStack stack = context.poseStack();
        stack.pushPose();
        Camera camera = ClientUtils.getCamera();
        Vec3 cameraPos = camera.position();
        Vector3f normal = this.to.subtract(this.from).toVector3f();
        context.submitNodeCollector().submitCustomGeometry(stack, RenderTypes.lines(), (poseStack, buffer) -> {
            buffer.addVertex(poseStack, MathUtils.move(this.from, cameraPos, 0.001F).toVector3f())
                    .setColor(this.red, this.green, this.blue, this.alpha)
                    .setNormal(poseStack, normal)
                    .setLineWidth(this.width);
            buffer.addVertex(poseStack, MathUtils.move(this.to, cameraPos, 0.001F).toVector3f())
                    .setColor(this.red, this.green, this.blue, this.alpha)
                    .setNormal(poseStack, normal)
                    .setLineWidth(this.width);
        });
        stack.popPose();
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public void setRgba(int rgba) {
        this.red = MathUtils.red(rgba);
        this.green = MathUtils.green(rgba);
        this.blue = MathUtils.blue(rgba);
        this.alpha = MathUtils.alpha(rgba);
    }

    @Override
    public void setWidth(float width) {
        this.width = width;
    }
}
