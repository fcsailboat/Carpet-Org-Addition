package boat.carpetorgaddition.mixin.network;

import boat.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory;
import boat.carpetorgaddition.wheel.screen.OldVersionPlayerInventoryScreenClientSide;
import boat.carpetorgaddition.wheel.screen.UnavailableSlotClientSide;
import boat.carpetorgaddition.wheel.screen.WithButtonScreenClientSide;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerMenu.class, priority = 1001)
public class ScreenHandlerMixin implements UnavailableSlotClientSide, WithButtonScreenClientSide, OldVersionPlayerInventoryScreenClientSide {
    @Shadow
    @Final
    public int containerId;
    @Unique
    private int from = 0;
    @Unique
    private int to = -1;
    @Unique
    private boolean withButtonMenu = false;
    @Unique
    private boolean oldPlayerInventoryScreen = false;

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (MathUtils.isInRange(this.from, this.to, slotIndex) || (this.withButtonMenu && WithButtonPlayerInventory.BUTTON_INDEX_LIST.contains(slotIndex))) {
            ci.cancel();
        }
    }

    @Override
    public void carpet_Org_Addition$sync(UnavailableSlotSyncS2CPacket pack) {
        if (this.containerId == pack.syncId()) {
            this.from = pack.from();
            this.to = pack.to();
        }
    }

    @Override
    public void carpet_Org_Addition$setWithButton() {
        this.withButtonMenu = true;
    }

    @Override
    public boolean carpet_Org_Addition$isWithButton() {
        return this.withButtonMenu;
    }

    @Override
    public void carpet_Org_Addition$setOldPlayerInventoryScreen() {
        this.oldPlayerInventoryScreen = true;
    }

    @Override
    public boolean carpet_Org_Addition$isOldPlayerInventoryScreen() {
        return this.oldPlayerInventoryScreen;
    }
}
