package boat.carpetorgaddition.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

public interface WorldRenderComponent {
    void render(LevelRenderContext context);

    boolean isStopped();
}
