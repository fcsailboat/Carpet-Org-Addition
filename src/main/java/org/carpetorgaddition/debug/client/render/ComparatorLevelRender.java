package org.carpetorgaddition.debug.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.carpetorgaddition.client.renderer.Tooltip;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.util.TextUtils;

/**
 * 比较器等级渲染器
 */
public class ComparatorLevelRender {
    @SuppressWarnings("DataFlowIssue")
    public static void render() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // 断言当前为开发环境
            ProductionEnvironmentError.assertDevelopmentEnvironment();
            if (DebugSettings.showComparatorLevel) {
                HitResult hitResult = client.crosshairTarget;
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    ServerWorld world = client.getServer().getWorld(client.player.getWorld().getRegistryKey());
                    BlockEntity blockEntity = world.getWorldChunk(blockPos).getBlockEntity(blockPos, WorldChunk.CreationType.IMMEDIATE);
                    if (blockEntity instanceof ComparatorBlockEntity comparator) {
                        int level = comparator.getOutputSignal();
                        if (level == 0) {
                            return;
                        }
                        Tooltip.drawTooltip(drawContext, TextUtils.createText("红石信号等级：" + level));
                    }
                }
            }
        });
    }
}