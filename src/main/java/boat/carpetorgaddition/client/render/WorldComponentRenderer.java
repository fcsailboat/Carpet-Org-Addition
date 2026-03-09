package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorldComponentRenderer {
    private static final Map<Object, WorldRenderComponent> COMPONENTS = new LinkedHashMap<>();

    static {
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> COMPONENTS.clear());
    }

    private WorldComponentRenderer() {
    }

    public static void add(int entityId, WorldRenderComponent component) {
        COMPONENTS.put(new EntityIdKey(entityId), component);
    }

    public static void remove(int entityId) {
        COMPONENTS.remove(entityId);
    }

    public static void render(LevelRenderContext context) {
        if (COMPONENTS.isEmpty()) {
            return;
        }
        COMPONENTS.entrySet().removeIf(entry -> entry.getValue().isStopped());
        PoseStack stack = context.poseStack();
        stack.pushPose();
        Camera camera = ClientUtils.getCamera();
        stack.translate(camera.position().reverse());
        COMPONENTS.values().forEach(component -> component.render(context));
        stack.popPose();
    }

    private record EntityIdKey(int id) {
    }
}
