package org.carpetorgaddition.client.renderer.beaconbox;

import net.minecraft.util.math.Box;

/**
 * 信标渲染盒大小修改器
 */
public class SizeModifier {
    final Box targetBox;
    final Box originalBox;
    final long timeMillis = System.currentTimeMillis();

    SizeModifier(Box targetBox, Box originalBox) {
        this.targetBox = targetBox;
        this.originalBox = originalBox;
    }
}
