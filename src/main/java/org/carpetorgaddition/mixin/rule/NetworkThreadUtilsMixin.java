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
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NetworkThreadUtils.class)
public class NetworkThreadUtilsMixin {
    @SuppressWarnings({"MixinExtrasOperationParameters", "unchecked"})
    @WrapOperation(method = "method_11072", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/Packet;apply(Lnet/minecraft/network/listener/PacketListener;)V"))
    private static <T extends PacketListener> void changeLog(final Packet<T> packet, final T listener, Operation<Void> original) {
        try {
            try {
                original.call(packet, listener);
            } catch (CrashException e) {
                if (e.getCause() instanceof CCEUpdateSuppressException cceUpdateSuppressException) {
                    throw cceUpdateSuppressException;
                } else {
                    // 被捕获的异常不是因为CCE引起的，重新抛出
                    throw e;
                }
            }
        } catch (CCEUpdateSuppressException e) {
            // TODO 是否应该重新抛出
            if (listener instanceof ServerPlayNetworkHandler networkHandler) {
                e.onCatch(networkHandler.player, (Packet<ServerPlayPacketListener>) packet);
            }
        }
    }
}
