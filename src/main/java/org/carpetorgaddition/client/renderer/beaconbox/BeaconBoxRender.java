package org.carpetorgaddition.client.renderer.beaconbox;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.carpetorgaddition.client.renderer.BoxRender;
import org.carpetorgaddition.util.MathUtils;
import org.jetbrains.annotations.NotNull;

public class BeaconBoxRender extends BoxRender {
    private SizeModifier sizeModifier;

    public BeaconBoxRender(@NotNull Box box) {
        super(box);
    }

    @Override
    public void render(MatrixStack matrixStack) {
        this.resize();
        super.render(matrixStack);
    }

    /**
     * 重新设置Box大小
     */
    private void resize() {
        if (this.sizeModifier == null) {
            return;
        }
        // 计算上次修改时间到当前时间的时间差
        long timeDifference = System.currentTimeMillis() - this.sizeModifier.timeMillis;
        if (timeDifference > 1500.0) {
            this.setBox(this.sizeModifier.targetBox);
            return;
        }
        // 计算立方体缩放因子
        double factor = Math.pow(timeDifference / 1500.0, 2);
        // 计算新立方体大小
        Box box = new Box(
                MathUtils.approach(this.sizeModifier.originalBox.minX, this.sizeModifier.targetBox.minX, factor),
                MathUtils.approach(this.sizeModifier.originalBox.minY, this.sizeModifier.targetBox.minY, factor),
                MathUtils.approach(this.sizeModifier.originalBox.minZ, this.sizeModifier.targetBox.minZ, factor),
                MathUtils.approach(this.sizeModifier.originalBox.maxX, this.sizeModifier.targetBox.maxX, factor),
                MathUtils.approach(this.sizeModifier.originalBox.maxY, this.sizeModifier.targetBox.maxY, factor),
                MathUtils.approach(this.sizeModifier.originalBox.maxZ, this.sizeModifier.targetBox.maxZ, factor)
        );
        this.setBox(box);
    }

    public void setSizeModifier(Box targetBox) {
        this.sizeModifier = new SizeModifier(targetBox, this.getBox());
    }

    public boolean noNeedToModify(Box box) {
        return this.sizeModifier != null && box.equals(this.sizeModifier.targetBox);
    }
}
