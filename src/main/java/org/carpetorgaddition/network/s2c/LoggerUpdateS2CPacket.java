package org.carpetorgaddition.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.carpetorgaddition.network.PacketUtils;
import org.jetbrains.annotations.Nullable;

// 记录器更新数据包
public record LoggerUpdateS2CPacket(
        String logName,
        @Nullable String option,
        boolean isRemove
) implements CustomPayload {
    public static final Id<LoggerUpdateS2CPacket> ID = PacketUtils.createId("logger_update");
    public static PacketCodec<RegistryByteBuf, LoggerUpdateS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public LoggerUpdateS2CPacket decode(RegistryByteBuf buf) {
            String logName = buf.readString();
            boolean isRemove = buf.readBoolean();
            String option = isRemove ? null : PacketUtils.readNullable(buf, PacketByteBuf::readString);
            return new LoggerUpdateS2CPacket(logName, option, isRemove);
        }

        @Override
        public void encode(RegistryByteBuf buf, LoggerUpdateS2CPacket value) {
            buf.writeString(value.logName);
            buf.writeBoolean(value.isRemove());
            if (value.isRemove()) {
                return;
            }
            PacketUtils.writeNullable(value.option, buf, () -> buf.writeString(value.option));
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
