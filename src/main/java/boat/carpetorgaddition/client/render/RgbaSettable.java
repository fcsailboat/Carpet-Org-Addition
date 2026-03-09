package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.util.MathUtils;
import net.minecraft.ChatFormatting;

public interface RgbaSettable {
    void setRgba(int rgba);

    default void setRgba(ChatFormatting formatting) {
        Integer color = formatting.getColor();
        if (color == null) {
            this.setRgba(-1);
        } else {
            this.setRgba(MathUtils.rgba(color, 255));
        }
    }
}
