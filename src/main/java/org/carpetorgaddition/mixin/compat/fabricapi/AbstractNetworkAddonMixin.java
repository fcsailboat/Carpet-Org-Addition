package org.carpetorgaddition.mixin.compat.fabricapi;

import carpet.patches.FakeClientConnection;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon;
import net.fabricmc.fabric.impl.networking.AbstractNetworkAddon;
import net.fabricmc.fabric.impl.networking.GlobalReceiverRegistry;
import net.minecraft.network.ClientConnection;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = AbstractNetworkAddon.class, priority = 998, remap = false)
public abstract class AbstractNetworkAddonMixin {
    @SuppressWarnings("RedundantIfStatement")
    @WrapWithCondition(method = "lateInit", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/networking/GlobalReceiverRegistry;startSession(Lnet/fabricmc/fabric/impl/networking/AbstractNetworkAddon;)V"))
    private boolean notStartSession_ifFakeClientConnection(GlobalReceiverRegistry<?> instance, AbstractNetworkAddon<?> addon) {
        // 修复fabric api和Carpet的内存泄漏问题
        if (CarpetOrgAdditionSettings.fakePlayerSpawnMemoryLeakFix && addon instanceof AbstractChanneledNetworkAddon<?>) {
            ClientConnection connection = ((AbstractChanneledNetworkAddonInvoker) addon).getConnection();
            if (connection instanceof FakeClientConnection) {
                return false;
            }
        }
        return true;
    }
}
