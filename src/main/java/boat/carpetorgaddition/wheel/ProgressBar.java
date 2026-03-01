package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import net.minecraft.network.chat.Component;

public class ProgressBar {
    private final double end;
    private double progress = 0.0;

    public ProgressBar(int end) {
        this.end = end;
    }

    public void setProgress(int current) {
        this.progress = Math.min(MathUtils.normalize(current, 0.0, this.end), 1);
    }

    public void setCompleted() {
        this.progress = 1.0;
    }

    public Component getDisplay() {
        return TextProvider.percentage(this.progress);
    }
}
