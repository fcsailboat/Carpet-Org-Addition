package boat.carpetorgaddition.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class CompositeWorldRenderComponent implements WorldRenderComponent {
    private final List<WorldRenderComponent> components;

    public CompositeWorldRenderComponent(List<? extends WorldRenderComponent> list) {
        this.components = new ArrayList<>(list);
    }

    @Override
    public void render(LevelRenderContext context) {
        this.components.removeIf(WorldRenderComponent::isStopped);
        this.components.forEach(component -> component.render(context));
    }

    @Override
    public boolean isStopped() {
        return this.components.stream().allMatch(WorldRenderComponent::isStopped);
    }
}
