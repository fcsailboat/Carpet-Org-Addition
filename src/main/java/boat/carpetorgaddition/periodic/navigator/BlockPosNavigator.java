package boat.carpetorgaddition.periodic.navigator;

import boat.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class BlockPosNavigator extends AbstractNavigator {
    protected final BlockPos blockPos;
    protected final Level world;

    public BlockPosNavigator(@NonNull ServerPlayer player, BlockPos blockPos, Level world) {
        super(player);
        this.blockPos = blockPos;
        this.world = world;
    }

    @Override
    public void tick() {
        Component text;
        if (ServerUtils.getWorld(this.player).equals(this.world)) {
            Component in = TextProvider.simpleBlockPos(this.blockPos);
            int distance = MathUtils.getBlockIntegerDistance(this.player.blockPosition(), this.blockPos);
            text = getHUDText(this.blockPos.getCenter(), in, distance);
        } else {
            text = TextBuilder.combineAll(TextProvider.dimension(this.world), TextProvider.simpleBlockPos(this.blockPos));
        }
        MessageUtils.sendMessageToHud(this.player, text);
    }

    @Override
    protected WaypointUpdateS2CPacket createPacket() {
        return new WaypointUpdateS2CPacket(this.blockPos, this.world);
    }

    @Override
    protected boolean updateRequired() {
        return false;
    }

    @Override
    public BlockPosNavigator copy(ServerPlayer player) {
        return new BlockPosNavigator(player, this.blockPos, this.world);
    }

    @Override
    protected boolean isArrive() {
        // 玩家与目的地在同一维度
        if (ServerUtils.getWorld(this.player).equals(this.world)) {
            if (MathUtils.getBlockIntegerDistance(this.player.blockPosition(), this.blockPos) <= 8) {
                // 到达目的地，停止追踪
                MessageUtils.sendMessageToHud(this.player, REACH.translate());
                this.clear();
                return true;
            }
        }
        return false;
    }
}
