package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;

import java.util.ArrayList;

public class WorldComponentRenderer {
    private static final ArrayList<WorldRenderComponent> COMPONENTS = new ArrayList<>();

    static {
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> COMPONENTS.clear());
    }

    private WorldComponentRenderer() {
    }

    public static void addRenderComponent(WorldRenderComponent component) {
        COMPONENTS.add(component);
    }

    public static void render(LevelRenderContext context) {
        if (COMPONENTS.isEmpty()) {
            return;
        }
        PoseStack stack = context.poseStack();
        stack.pushPose();
        Camera camera = ClientUtils.getCamera();
        stack.translate(camera.position().reverse());
        COMPONENTS.forEach(component -> component.render(context));
        stack.popPose();
    }
}
