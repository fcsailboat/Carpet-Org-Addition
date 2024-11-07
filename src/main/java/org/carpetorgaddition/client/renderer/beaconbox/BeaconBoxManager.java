package org.carpetorgaddition.client.renderer.beaconbox;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.carpetorgaddition.network.s2c.BeaconBoxUpdateS2CPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BeaconBoxManager {
    private static final HashMap<BlockPos, BeaconBoxRender> BEACON_RENDER = new HashMap<>();

    // 注册渲染器
    public static void register() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> frame(context.matrixStack(), context.world()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BEACON_RENDER.clear());
    }

    // 调用渲染器
    public static void frame(MatrixStack matrixStack, ClientWorld world) {
        Iterator<Map.Entry<BlockPos, BeaconBoxRender>> iterator = BEACON_RENDER.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BeaconBoxRender> entry = iterator.next();
            BlockState blockState = world.getBlockState(entry.getKey());
            if (blockState == null || !blockState.isOf(Blocks.BEACON)) {
                // 方块被破坏或离开渲染区域，删除渲染器
                iterator.remove();
            }
            entry.getValue().render(matrixStack);
        }
    }

    // 设置渲染器
    public static void setBeaconRender(BlockPos blockPos, Box box) {
        // 清除单个信标渲染
        if (BeaconBoxUpdateS2CPacket.ZERO.equals(box)) {
            BEACON_RENDER.remove(blockPos);
            return;
        }
        BeaconBoxRender beaconBoxRender = BEACON_RENDER.get(blockPos);
        if (beaconBoxRender == null) {
            // 首次添加渲染器
            BEACON_RENDER.put(blockPos, new BeaconBoxRender(box));
            return;
        }
        if (beaconBoxRender.noNeedToModify(box)) {
            // 信标范围未修改
            return;
        }
        // 信标范围已修改，设置大小修改器
        beaconBoxRender.setSizeModifier(box);
    }

    public static void clearRender() {
        BEACON_RENDER.clear();
    }
}
