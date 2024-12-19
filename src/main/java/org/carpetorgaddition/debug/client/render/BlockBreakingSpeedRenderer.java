package org.carpetorgaddition.debug.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.client.renderer.Tooltip;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.util.TextUtils;

public class BlockBreakingSpeedRenderer {
    @SuppressWarnings("DataFlowIssue")
    public static void render() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // 断言当前为开发环境
            ProductionEnvironmentError.assertDevelopmentEnvironment();
            if (DebugSettings.showBlockBreakingSpeed) {
                HitResult hitResult = client.crosshairTarget;
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    ServerWorld world = client.getServer().getWorld(client.player.getWorld().getRegistryKey());
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.isAir()) {
                        return;
                    }
                    float speed = client.player.getBlockBreakingSpeed(blockState);
                    String formatted = "%.2f".formatted(speed);
                    if (formatted.endsWith(".00")) {
                        formatted = formatted.substring(0, formatted.length() - 3);
                    } else if (formatted.endsWith("0")) {
                        formatted = formatted.substring(0, formatted.length() - 1);
                    }
                    Tooltip.drawTooltip(drawContext, TextUtils.createText("挖掘速度：" + formatted));
                }
            }
        });
    }
}
