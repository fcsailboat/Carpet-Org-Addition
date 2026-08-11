package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.wheel.ColorValue;
import net.minecraft.ChatFormatting;

public interface RgbaSettable {
    void setRgba(int rgba);

    default void setRgba(ChatFormatting formatting) {
        Integer color = formatting.getColor();
        if (color == null) {
            this.setRgba(-1);
        } else {
            this.setRgba(ColorValue.fromRgba(color, 255).toRgba());
        }
    }
}
