package org.carpetorgaddition.mixin.util;

import net.minecraft.client.gl.ShaderProgramLayer;
import net.minecraft.client.gl.ShaderProgramLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderProgramLayers.class)
public interface ShaderProgramLayersAccessor {
    @Accessor("RENDERTYPE_LINES")
    static ShaderProgramLayer.Stage getRenderTypeLines() {
        throw new AssertionError();
    }
}
