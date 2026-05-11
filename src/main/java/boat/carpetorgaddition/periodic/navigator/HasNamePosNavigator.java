package boat.carpetorgaddition.periodic.navigator;

import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class HasNamePosNavigator extends BlockPosNavigator {
    private final Component name;

    public HasNamePosNavigator(@NotNull ServerPlayer player, BlockPos blockPos, Level world, Component name) {
        super(player, blockPos, world);
        this.name = name;
    }

    @Override
    public void tick() {
        Component text;
        Component pos = TextProvider.simpleBlockPos(this.blockPos);
        // 玩家与目的地是否在同一维度
        if (ServerUtils.getWorld(this.player).equals(this.world)) {
            int distance = MathUtils.getBlockIntegerDistance(this.player.blockPosition(), this.blockPos);
            text = getHUDText(ServerUtils.getBlockCenter(this.blockPos), IN.translate(this.name, pos), distance);
        } else {
            text = IN.translate(this.name, TextBuilder.combineAll(TextProvider.dimension(this.world), pos));
        }
        MessageUtils.sendMessageToHud(this.player, text);
    }

    @Override
    public HasNamePosNavigator copy(ServerPlayer player) {
        return new HasNamePosNavigator(player, this.blockPos, this.world, this.name);
    }
}
