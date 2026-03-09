package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

// 记录器更新数据包
public record LoggerUpdateS2CPacket(String name, @Nullable String option, boolean remove) implements CustomPacketPayload {
    public static final Type<LoggerUpdateS2CPacket> ID = NetworkPacketRegister.ofType("logger_update");
    public static final StreamCodec<RegistryFriendlyByteBuf, LoggerUpdateS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public LoggerUpdateS2CPacket decode(RegistryFriendlyByteBuf buf) {
            String name = buf.readUtf();
            String option = buf.readNullable(FriendlyByteBuf::readUtf);
            boolean remove = buf.readBoolean();
            return new LoggerUpdateS2CPacket(name, option, remove);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, LoggerUpdateS2CPacket value) {
            buf.writeUtf(value.name);
            buf.writeNullable(value.option, FriendlyByteBuf::writeUtf);
            buf.writeBoolean(value.remove());
        }
    };

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
