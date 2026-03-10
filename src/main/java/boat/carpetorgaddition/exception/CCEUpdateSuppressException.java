package boat.carpetorgaddition.exception;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;

public class CCEUpdateSuppressException extends ClassCastException {
    private final BlockPos triggerPos;

    public CCEUpdateSuppressException(BlockPos blockPos, String message) {
        super(message);
        this.triggerPos = blockPos;
    }

    /**
     * 在日志中输出造成异常的玩家和异常原因以及位置
     *
     * @param player 造成异常的玩家
     * @param packet 造成异常的数据包
     * @apiNote 如果启用了 {@code Carpet TIS Addition} 的{@code 阻止更新抑制崩溃}，可能导致异常提前被捕获
     */
    public void onCatch(ServerPlayer player, Packet<ServerGamePacketListener> packet) {
        StringBuilder builder = new StringBuilder();
        builder.append(PlayerUtils.getName(player)).append(" triggered CCE update suppression while ");
        if (packet instanceof ServerboundPlayerActionPacket actionC2SPacket) {
            // 破坏方块
            switch (actionC2SPacket.getAction()) {
                // 不应该会执行到其他case块
                // 不获取方块名称是因为此时方块可能已经被破坏，不获取物品名称也是同理
                case START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK -> builder.append("breaking a block");
                case DROP_ALL_ITEMS, DROP_ITEM -> builder.append("dropping an item");
                case RELEASE_USE_ITEM -> builder.append("using an item");
                case SWAP_ITEM_WITH_OFFHAND -> builder.append("swapping main-hand and off-hand items");
                default -> throw new IllegalStateException();
            }
        } else if (packet instanceof ServerboundUseItemOnPacket) {
            // 放置或交互方块
            builder.append("placing or interacting with a block");
        } else {
            // 其它异常
            builder.append("sending a ").append(packet.getClass().getSimpleName()).append(" packet");
        }
        String worldPos = ServerUtils.toWorldPosString(ServerUtils.getWorld(player), this.triggerPos);
        builder.append(" at ").append(worldPos);
        CarpetOrgAddition.LOGGER.info(builder.toString());
    }
}
