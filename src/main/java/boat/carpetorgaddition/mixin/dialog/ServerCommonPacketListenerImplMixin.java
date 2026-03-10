package boat.carpetorgaddition.mixin.dialog;

import boat.carpetorgaddition.network.event.CustomClickActionContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @WrapMethod(method = "handleCustomClickAction")
    private void handleCustomClickAction(ServerboundCustomClickActionPacket packet, Operation<Void> original) {
        if ((Object) this instanceof ServerGamePacketListenerImpl listener) {
            ScopedValue.where(CustomClickActionContext.CURRENT_PLAYER, listener.player).run(() -> original.call(packet));
        } else {
            original.call(packet);
        }
    }
}
