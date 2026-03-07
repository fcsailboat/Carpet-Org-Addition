package boat.carpetorgaddition.client.renderer.waypoint;

import boat.carpetorgaddition.client.CarpetOrgAdditionClient;
import boat.carpetorgaddition.client.util.ClientKeyBindingUtils;
import boat.carpetorgaddition.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class HighlightWaypoint extends Waypoint {
    /**
     * 路径点开始闪烁的时间
     */
    private static final long FLICKER_TIME = 200L;
    /**
     * 路径点是否以恒定的频率闪烁
     */
    private static final boolean CONSTANT_FLICKER = true;

    public HighlightWaypoint(Level world, Vec3 vec3d, long duration, boolean persistent) {
        super(world, vec3d, Waypoint.HIGHLIGHT, duration, persistent);
    }

    @Override
    public void render(PoseStack poseStack, SubmitNodeCollector collector, Camera camera, DeltaTracker deltaTracker) {
        if (ClientKeyBindingUtils.isPressed(CarpetOrgAdditionClient.CLEAR_WAYPOINT) && ClientUtils.getCurrentScreen() == null) {
            this.stop();
        }
        super.render(poseStack, collector, camera, deltaTracker);
    }

    @Override
    protected float getRenderAlpha() {
        long remaining = this.getRemaining();
        if (this.isPersistent() || remaining > FLICKER_TIME || remaining <= 0) {
            return 1F;
        }
        double time = FLICKER_TIME - (double) remaining + this.tickDelta;
        double flicker = time / FLICKER_TIME;
        double speed = CONSTANT_FLICKER ? 1.0 : 1 + Math.pow(flicker, 2.0) * 1.2;
        double x = (0.5 * speed * time) + (Math.PI / 2.0);
        return (float) Math.max(((Math.sin(x) / 2.0)) + 0.5F, 0.0);
    }

    @Override
    public String getName() {
        return "Highlight";
    }
}
