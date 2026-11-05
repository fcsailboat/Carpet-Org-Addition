package boat.carpetorgaddition.mixin.debug.accessor;

import boat.carpetorgaddition.debug.OnlyDeveloped;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("UnusedReturnValue")
@OnlyDeveloped
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable> T putDrawable(T drawable);
}
