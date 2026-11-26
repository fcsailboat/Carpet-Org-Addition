package boat.carpetorgaddition.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelSubmitContext;

public interface WorldRenderComponent {
    void render(LevelSubmitContext context);

    boolean isStopped();
}
