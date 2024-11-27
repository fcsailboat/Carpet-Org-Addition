package org.carpetorgaddition.mixin.network.unavailableslotsync;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.screen.UnavailableSlotImplInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin implements UnavailableSlotImplInterface {
    @Shadow
    @Final
    public int syncId;
    @Unique
    private int from = 0;
    @Unique
    private int to = -1;

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (MathUtils.isInRange(this.from, this.to, slotIndex)) {
            ci.cancel();
        }
    }

    @Override
    public void sync(UnavailableSlotSyncS2CPacket pack) {
        if (this.syncId == pack.syncId()) {
            this.from = pack.from();
            this.to = pack.to();
        }
    }
}
