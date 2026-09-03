package boat.carpetorgaddition.client.render.waypoint;

import boat.carpetorgaddition.client.util.ClientCommandUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.common.CommonCommands;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class NavigatorWaypoint extends Waypoint {
    /**
     * 插值动画进度，上次设置目标后到现在经过的时间
     */
    private double progress = 0d;

    public NavigatorWaypoint(ResourceKey<Level> registryKey, Vec3 vec3d) {
        super(registryKey, vec3d, Waypoint.NAVIGATOR, 1, true);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Matrix4fc modelView, Matrix4f projection) {
        // 计算帧间时间增量
        double delta = this.tickDelta - this.lastTickDelta;
        if (delta < 0.0) {
            // 处理时间回绕
            delta = delta - Math.floor(delta);
        }
        this.progress += delta;
        super.render(graphics, modelView, projection);
    }

    @Override
    protected Vec3 getInterpolation() {
        if (this.progress >= 1d) {
            return super.getInterpolation();
        }
        return MathUtils.approach(this.lastTarget, this.getTarget(), this.progress);
    }

    @Override
    public void setTarget(ResourceKey<Level> registryKey, Vec3 vec3d) {
        if (registryKey.equals(this.registryKey)) {
            this.lastTarget = this.getTarget();
            this.progress = 0d;
        }
        super.setTarget(registryKey, vec3d);
    }

    @Override
    public void requestServerToStop() {
        ClientCommandUtils.sendCommand(CommonCommands.stopNavigate());
    }

    @Override
    public String getName() {
        return "Waypoint";
    }
}
