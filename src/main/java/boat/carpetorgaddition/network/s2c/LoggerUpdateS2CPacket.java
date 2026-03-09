package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

// 记录器更新数据包
public record LoggerUpdateS2CPacket(String logName, @Nullable String option, boolean isRemove) implements CustomPacketPayload {
    public static final Type<LoggerUpdateS2CPacket> ID = NetworkUtils.createPacketId("logger_update");
    public static final StreamCodec<RegistryFriendlyByteBuf, LoggerUpdateS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public LoggerUpdateS2CPacket decode(RegistryFriendlyByteBuf buf) {
            String logName = buf.readUtf();
            boolean isRemove = buf.readBoolean();
            String option = isRemove ? null : NetworkUtils.readNullable(buf, FriendlyByteBuf::readUtf);
            return new LoggerUpdateS2CPacket(logName, option, isRemove);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, LoggerUpdateS2CPacket value) {
            buf.writeUtf(value.logName);
            buf.writeBoolean(value.isRemove());
            if (value.isRemove()) {
                return;
            }
            NetworkUtils.writeNullable(value.option, buf, buf::writeUtf);
        }
    };

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
