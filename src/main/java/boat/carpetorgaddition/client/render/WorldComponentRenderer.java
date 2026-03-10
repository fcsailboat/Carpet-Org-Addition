package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.ServerUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorldComponentRenderer {
    private static final Map<Identifier, Map<Object, WorldRenderComponent>> COMPONENTS = new LinkedHashMap<>();
    public static final Identifier ENTITY_ID_KEY = ServerUtils.ofIdentifier("entity_id_key");

    static {
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> COMPONENTS.clear());
    }

    private WorldComponentRenderer() {
    }

    public static void add(Identifier identifier, Object key, WorldRenderComponent component) {
        Map<Object, WorldRenderComponent> map = COMPONENTS.computeIfAbsent(identifier, _ -> new LinkedHashMap<>());
        map.put(key, component);
    }

    public static void remove(Identifier identifier, Object key) {
        Map<Object, WorldRenderComponent> map = COMPONENTS.get(identifier);
        if (map == null) {
            return;
        }
        map.remove(key);
    }

    public static void remove(Identifier identifier) {
        COMPONENTS.remove(identifier);
    }

    public static void render(LevelRenderContext context) {
        if (COMPONENTS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Identifier, Map<Object, WorldRenderComponent>>> it = COMPONENTS.entrySet().iterator();
        while (it.hasNext()) {
            Map<Object, WorldRenderComponent> map = it.next().getValue();
            map.entrySet().removeIf(entry -> entry.getValue().isStopped());
            if (map.isEmpty()) {
                it.remove();
            }
        }
        PoseStack stack = context.poseStack();
        stack.pushPose();
        Camera camera = ClientUtils.getCamera();
        stack.translate(camera.position().reverse());
        COMPONENTS.values().forEach(map -> map.values().forEach(component -> component.render(context)));
        stack.popPose();
    }
}
