package boat.carpetorgaddition.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public class BoxRenderComponent implements WorldRenderComponent, RgbaSettable {
    @Unmodifiable
    private final List<PlaneRenderComponent> components;

    public BoxRenderComponent(AABB box) {
        this.components = ShapePlane.ofPlaces(box).stream().map(PlaneRenderComponent::new).toList();
    }

    @Override
    public void render(LevelRenderContext context) {
        this.components.forEach(component -> component.render(context));
    }

    @Override
    public boolean isStopped() {
        return this.components.stream().allMatch(PlaneRenderComponent::isStopped);
    }

    @Override
    public void setRgba(int rgba) {
        this.components.forEach(component -> component.setRgba(rgba));
    }
}
