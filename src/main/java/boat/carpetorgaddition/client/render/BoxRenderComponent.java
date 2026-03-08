package boat.carpetorgaddition.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Unmodifiable;

public class BoxRenderComponent implements WorldRenderComponent {
    @Unmodifiable
    private final CompositeWorldRenderComponent component;

    public BoxRenderComponent(AABB box) {
        this.component = new CompositeWorldRenderComponent(ShapePlane.ofPlaces(box).stream().map(PlaneRenderComponent::new).toList());
    }

    @Override
    public void render(LevelRenderContext context) {
        this.component.render(context);
    }

    @Override
    public boolean isStopped() {
        return this.component.isStopped();
    }
}
