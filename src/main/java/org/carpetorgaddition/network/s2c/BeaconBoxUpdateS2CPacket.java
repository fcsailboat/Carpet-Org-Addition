package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.carpetorgaddition.CarpetOrgAddition;

/**
 * 信标范围更新数据包
 */
public record BeaconBoxUpdateS2CPacket(BlockPos blockPos, Box box) implements CustomPayload {
    public static final Box ZERO = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final Identifier BEACON_BOX_UPDATE = Identifier.of(CarpetOrgAddition.MOD_ID, "beacon_box_update");
    public static final Id<BeaconBoxUpdateS2CPacket> ID = new Id<>(BEACON_BOX_UPDATE);
    public static PacketCodec<RegistryByteBuf, BeaconBoxUpdateS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public BeaconBoxUpdateS2CPacket decode(RegistryByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            double minX = buf.readDouble();
            double minY = buf.readDouble();
            double minZ = buf.readDouble();
            double maxX = buf.readDouble();
            double maxY = buf.readDouble();
            double maxZ = buf.readDouble();
            Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
            return new BeaconBoxUpdateS2CPacket(pos, box);
        }

        @Override
        public void encode(RegistryByteBuf buf, BeaconBoxUpdateS2CPacket value) {
            buf.writeBlockPos(value.blockPos());
            Box box = value.box();
            buf.writeDouble(box.minX);
            buf.writeDouble(box.minY);
            buf.writeDouble(box.minZ);
            buf.writeDouble(box.maxX);
            buf.writeDouble(box.maxY);
            buf.writeDouble(box.maxZ);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
