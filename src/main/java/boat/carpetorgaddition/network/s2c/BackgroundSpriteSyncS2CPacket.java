package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

/**
 * 背景精灵同步数据包
 */
@NullMarked
public record BackgroundSpriteSyncS2CPacket(int syncId, int slotIndex, Identifier identifier) implements CustomPacketPayload {
    public static final Type<BackgroundSpriteSyncS2CPacket> ID = NetworkPacketRegister.ofType("background_sprite_sync");

    public static final StreamCodec<RegistryFriendlyByteBuf, BackgroundSpriteSyncS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public BackgroundSpriteSyncS2CPacket decode(RegistryFriendlyByteBuf buf) {
            int syncId = buf.readInt();
            int slotIndex = buf.readInt();
            Identifier identifier = buf.readIdentifier();
            return new BackgroundSpriteSyncS2CPacket(syncId, slotIndex, identifier);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BackgroundSpriteSyncS2CPacket value) {
            buf.writeInt(value.syncId);
            buf.writeInt(value.slotIndex);
            buf.writeIdentifier(value.identifier);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
