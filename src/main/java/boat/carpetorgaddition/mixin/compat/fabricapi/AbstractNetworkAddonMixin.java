package boat.carpetorgaddition.mixin.compat.fabricapi;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.mixin.accessor.fabricapi.AbstractChanneledNetworkAddonAccessor;
import carpet.patches.FakeClientConnection;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon;
import net.fabricmc.fabric.impl.networking.AbstractNetworkAddon;
import net.fabricmc.fabric.impl.networking.GlobalReceiverRegistry;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractNetworkAddon.class, priority = 998, remap = false)
public abstract class AbstractNetworkAddonMixin {
    /**
     * 修复{@code fabric api}和{@code Carpet}的内存泄漏问题
     *
     * @see <a href="https://github.com/FabricMC/fabric/issues/3974">Memory leaks occur when players log in or log out.</a>
     */
    @SuppressWarnings("RedundantIfStatement")
    @WrapWithCondition(method = "lateInit", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/networking/GlobalReceiverRegistry;startSession(Lnet/fabricmc/fabric/impl/networking/AbstractNetworkAddon;)V"))
    private boolean notStartSession_ifFakeClientConnection(GlobalReceiverRegistry<?> instance, AbstractNetworkAddon<?> addon) {
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SPAWN_MEMORY_LEAK_FIX.value() && addon instanceof AbstractChanneledNetworkAddon<?>) {
            Connection connection = ((AbstractChanneledNetworkAddonAccessor) addon).getConnection();
            if (connection instanceof FakeClientConnection) {
                return false;
            }
        }
        return true;
    }
}
