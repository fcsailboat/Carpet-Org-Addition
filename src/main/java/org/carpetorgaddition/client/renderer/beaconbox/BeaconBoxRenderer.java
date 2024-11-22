package org.carpetorgaddition.client.renderer.beaconbox;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.carpetorgaddition.client.renderer.BoxRenderer;
import org.carpetorgaddition.client.renderer.WorldRenderer;
import org.carpetorgaddition.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BeaconBoxRenderer extends BoxRenderer implements WorldRenderer {
    private SizeModifier sizeModifier;
    private final BlockPos blockPos;

    public BeaconBoxRenderer(BlockPos blockPos, @NotNull Box box) {
        super(box);
        this.blockPos = blockPos;
    }

    @Override
    public void render(WorldRenderContext context) {
        this.resize();
        super.render(Objects.requireNonNull(context.matrixStack()));
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
        double x = timeDifference / 1500.0;
        double factor = x < 0.5 ? 2 * Math.pow(x, 2) : 1 - Math.pow(-2 * x + 2, 2) / 2;
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
        if (this.sizeModifier != null && targetBox.equals(this.sizeModifier.targetBox)) {
            return;
        }
        this.sizeModifier = new SizeModifier(targetBox, this.getBox());
    }

    @Override
    public boolean shouldStop() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return true;
        }
        BlockState blockState = world.getBlockState(this.blockPos);
        return blockState == null || !blockState.isOf(Blocks.BEACON);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && this.getClass() == o.getClass() && this.blockPos.equals(((BeaconBoxRenderer) o).blockPos);
    }

    @Override
    public int hashCode() {
        return this.blockPos.hashCode();
    }
}
