package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.wheel.common.CommonTexts;
import net.minecraft.network.chat.Component;

public class ProgressBar {
    private final double end;
    private double progress = 0.0;

    public ProgressBar(int end) {
        this.end = end;
    }

    public ProgressBar(long end) {
        this.end = end;
    }

    public void setProgress(long current) {
        this.progress = Math.min((double) current / (this.end - 0.0), 1);
    }

    public void setCompleted() {
        this.progress = 1.0;
    }

    public Component getDisplay() {
        return CommonTexts.percentage(this.progress);
    }
}
