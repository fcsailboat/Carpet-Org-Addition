package org.carpetorgaddition.periodic.navigator;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.jetbrains.annotations.NotNull;

public class HasNamePosNavigator extends BlockPosNavigator {
    private final Text name;

    public HasNamePosNavigator(@NotNull ServerPlayerEntity player, BlockPos blockPos, World world, Text name) {
        super(player, blockPos, world);
        this.name = name;
    }

    @Override
    public void tick() {
        if (this.terminate()) {
            this.clear();
            return;
        }
        MutableText text;
        MutableText posText = TextConstants.simpleBlockPos(this.blockPos);
        // 玩家与目的地是否在同一维度
        if (this.player.getWorld().equals(this.world)) {
            MutableText distance = TextUtils.translate(DISTANCE, MathUtils.getBlockIntegerDistance(this.player.getBlockPos(), this.blockPos));
            text = getHUDText(this.blockPos.toCenterPos(), TextUtils.translate(IN, this.name, posText), distance);
        } else {
            text = TextUtils.translate(IN, this.name, TextUtils.appendAll(WorldUtils.getDimensionName(this.world), posText));
        }
        MessageUtils.sendMessageToHud(this.player, text);
    }

    @Override
    public HasNamePosNavigator copy(ServerPlayerEntity player) {
        return new HasNamePosNavigator(player, this.blockPos, this.world, this.name);
    }
}
