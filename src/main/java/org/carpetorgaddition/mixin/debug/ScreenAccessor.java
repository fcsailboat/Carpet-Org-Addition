package org.carpetorgaddition.mixin.debug;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("UnusedReturnValue")
@OnlyDeveloped
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("addDrawable")
    <T extends Drawable> T putDrawable(T drawable);
}
