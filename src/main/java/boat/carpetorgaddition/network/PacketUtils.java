package boat.carpetorgaddition.network;

import boat.carpetorgaddition.CarpetOrgAdditionMetadata;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class PacketUtils {
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createId(String path) {
        Identifier identifier = Identifier.fromNamespaceAndPath(CarpetOrgAdditionMetadata.MOD_ID, path);
        return new CustomPacketPayload.Type<>(identifier);
    }

    /**
     * 向数据包存入一个可以为空的对象
     */
    public static <T> void writeNullable(@Nullable T value, RegistryFriendlyByteBuf buf, Consumer<@NotNull T> consumer) {
        if (value == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            consumer.accept(value);
        }
    }

    /**
     * 从网络数据包读取一个可能为null的对象
     */
    @Nullable
    public static <T> T readNullable(RegistryFriendlyByteBuf buf, Function<RegistryFriendlyByteBuf, T> function) {
        return buf.readBoolean() ? function.apply(buf) : null;
    }
}
