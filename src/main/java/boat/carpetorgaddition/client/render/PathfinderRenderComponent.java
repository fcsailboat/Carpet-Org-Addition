package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PathfinderRenderComponent implements WorldRenderComponent {
    private final int entityId;
    private final List<BoxRenderComponent> points;
    private final List<LineRenderComponent> lines;
    private static final Vec3 RADIUS = new Vec3(0.1, 0.1, 0.1);

    public PathfinderRenderComponent(int entityId, List<Vec3> list) {
        this.entityId = entityId;
        this.points = list.stream().map(vec3 -> new AABB(vec3.add(RADIUS), vec3.subtract(RADIUS))).map(BoxRenderComponent::new).toList();
        ArrayList<LineRenderComponent> lines = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            lines.add(new LineRenderComponent(list.get(i), list.get(i + 1)));
        }
        this.lines = lines;
        this.lines.forEach(line -> line.setRgba(ChatFormatting.YELLOW));
        this.points.forEach(point -> point.setRgba(ChatFormatting.GREEN));
    }

    @Override
    public void render(LevelRenderContext context) {
        this.points.forEach(component -> component.render(context));
        this.lines.forEach(component -> component.render(context));
    }

    @Override
    public boolean isStopped() {
        if (this.points.isEmpty()) {
            return true;
        }
        if (this.points.stream().allMatch(WorldRenderComponent::isStopped)) {
            return true;
        }
        if (this.lines.stream().allMatch(WorldRenderComponent::isStopped)) {
            return true;
        }
        return ClientUtils.getEntity(this.entityId).map(Entity::isRemoved).orElse(true);
    }
}
