package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.crash.CrashException;
import org.carpetorgaddition.exception.CCEUpdateSuppressException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NetworkThreadUtils.class)
public class NetworkThreadUtilsMixin {
    @SuppressWarnings({"MixinExtrasOperationParameters", "unchecked"})
    @WrapOperation(method = "method_11072", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/Packet;apply(Lnet/minecraft/network/listener/PacketListener;)V"))
    private static <T extends PacketListener> void exceptionReason(final Packet<T> packet, final T listener, Operation<Void> original) {
        try {
            original.call(packet, listener);
        } catch (RuntimeException e) {
            CCEUpdateSuppressException cce;
            if (e instanceof CCEUpdateSuppressException) {
                cce = (CCEUpdateSuppressException) e;
            } else if (e instanceof CrashException crashException && crashException.getCause() instanceof CCEUpdateSuppressException exception) {
                cce = exception;
            } else {
                throw e;
            }
            exceptionReason((Packet<ServerPlayPacketListener>) packet, listener, cce);
            throw e;
        }
    }

    // 如果是玩家操作导致的，记录异常原因
    @Unique
    private static <T extends PacketListener> void exceptionReason(Packet<ServerPlayPacketListener> packet, T listener, CCEUpdateSuppressException cceUpdateSuppressException) {
        if (listener instanceof ServerPlayNetworkHandler networkHandler) {
            cceUpdateSuppressException.onCatch(networkHandler.player, packet);
        }
    }
}
