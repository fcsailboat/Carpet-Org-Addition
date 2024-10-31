package org.carpetorgaddition.debug.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.carpetorgaddition.client.renderer.Tooltip;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.wheel.Counter;

import java.util.ArrayList;
import java.util.List;

/**
 * 灵魂沙物品计数渲染器
 */
public class SoulSandItemCountRender {
    @SuppressWarnings("DataFlowIssue")
    public static void render() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // 断言当前为开发环境
            ProductionEnvironmentError.assertDevelopmentEnvironment();
            if (show(client)) {
                HitResult hitResult = client.crosshairTarget;
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    BlockState blockState = client.world.getBlockState(blockPos);
                    if (blockState.isAir()) {
                        return;
                    }
                    if (blockState.isOf(Blocks.SOUL_SAND)) {
                        Box box = new Box(blockPos.up());
                        List<ItemEntity> entities = client.world.getEntitiesByClass(ItemEntity.class, box, EntityPredicates.VALID_ENTITY);
                        if (entities.isEmpty()) {
                            return;
                        }
                        Counter<Item> counter = new Counter<>();
                        for (ItemEntity itemEntity : entities) {
                            ItemStack itemStack = itemEntity.getStack();
                            counter.add(itemStack.getItem(), itemStack.getCount());
                        }
                        List<Text> list = new ArrayList<>();
                        for (Item item : counter) {
                            int count = counter.getCount(item);
                            list.add(TextUtils.appendAll(item.getName(), "*", count));
                        }
                        Tooltip.drawTooltip(drawContext, list);
                    }
                }
            }
        });
    }

    private static boolean show(MinecraftClient client) {
        if (DebugSettings.showSoulSandItemCount) {
            if (client.getServer() == null) {
                return false;
            }
            if (client.currentScreen == null) {
                return true;
            }
            return client.currentScreen instanceof ChatScreen;
        }
        return false;
    }
}
