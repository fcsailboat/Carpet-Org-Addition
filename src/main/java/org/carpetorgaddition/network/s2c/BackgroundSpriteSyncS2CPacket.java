package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;

/**
 * 背景精灵同步数据包
 */
public record BackgroundSpriteSyncS2CPacket(int syncId, int slotIndex, Identifier identifier) implements CustomPayload {
    private static final Identifier BACKGROUND_SPRITE_SYNC = Identifier.of(CarpetOrgAddition.MOD_ID, "background_sprite_sync");
    public static final Id<BackgroundSpriteSyncS2CPacket> ID = new Id<>(BACKGROUND_SPRITE_SYNC);

    public static PacketCodec<RegistryByteBuf, BackgroundSpriteSyncS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public BackgroundSpriteSyncS2CPacket decode(RegistryByteBuf buf) {
            int syncId = buf.readInt();
            int slotIndex = buf.readInt();
            Identifier identifier = buf.readIdentifier();
            return new BackgroundSpriteSyncS2CPacket(syncId, slotIndex, identifier);
        }

        @Override
        public void encode(RegistryByteBuf buf, BackgroundSpriteSyncS2CPacket value) {
            buf.writeInt(value.syncId);
            buf.writeInt(value.slotIndex);
            buf.writeIdentifier(value.identifier);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
