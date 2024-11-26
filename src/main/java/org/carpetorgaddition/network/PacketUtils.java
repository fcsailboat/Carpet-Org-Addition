package org.carpetorgaddition.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class PacketUtils {
    public static <T extends CustomPayload> CustomPayload.Id<T> createId(String path) {
        Identifier identifier = Identifier.of(CarpetOrgAddition.MOD_ID, path);
        return new CustomPayload.Id<>(identifier);
    }

    /**
     * 向数据包存入一个可以为空的对象
     */
    public static <T> void writeNullable(@Nullable T value, RegistryByteBuf buf, Runnable runnable) {
        if (value == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            runnable.run();
        }
    }

    /**
     * 从网络数据包读取一个可能为null的对象
     */
    @Nullable
    public static <T> T readNullable(RegistryByteBuf buf, Function<RegistryByteBuf, T> function) {
        return buf.readBoolean() ? function.apply(buf) : null;
    }
}
