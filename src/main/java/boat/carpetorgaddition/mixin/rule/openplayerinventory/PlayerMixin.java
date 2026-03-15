package boat.carpetorgaddition.mixin.rule.openplayerinventory;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryAccessor;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import carpet.patches.EntityPlayerMPFake;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Unique
    private final Player self = (Player) (Object) this;

    @Inject(method = "interactOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private void openPlayerInventory(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof Player interviewee) {
            PlayerInventoryType type = this.self.isShiftKeyDown() ? PlayerInventoryType.ENDER_CHEST : PlayerInventoryType.INVENTORY;
            switch (CarpetOrgAdditionSettings.OPEN_PLAYER_INVENTORY.value()) {
                case FALSE -> {
                }
                case FAKE_PLAYER -> {
                    if (interviewee instanceof EntityPlayerMPFake fakePlayer) {
                        PlayerInventoryAccessor accessor = new PlayerInventoryAccessor(fakePlayer, (ServerPlayer) this.self);
                        PlayerUtils.openScreenHandler(
                                this.self,
                                (containerId, inventory, serverPlayer) -> accessor.createMenu(containerId, inventory, serverPlayer, type),
                                accessor.getDisplayName()
                        );
                        cir.setReturnValue(InteractionResult.SUCCESS);
                    }
                }
                case ANY_PLAYER -> {
                    if (interviewee instanceof ServerPlayer player) {
                        PlayerInventoryAccessor accessor = new PlayerInventoryAccessor(player, (ServerPlayer) this.self);
                        PlayerUtils.openScreenHandler(
                                this.self,
                                (containerId, inventory, serverPlayer) -> accessor.createMenu(containerId, inventory, serverPlayer, type),
                                accessor.getDisplayName()
                        );
                        cir.setReturnValue(InteractionResult.SUCCESS);
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "interactOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private void openPlayerInventoryClient(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof Player player) {
            switch (CarpetOrgAdditionSettings.OPEN_PLAYER_INVENTORY.value()) {
                case FALSE -> {
                }
                case FAKE_PLAYER -> {
                    if (ClientUtils.isFakePlayer(player)) {
                        cir.setReturnValue(InteractionResult.SUCCESS);
                    }
                }
                case ANY_PLAYER -> {
                    if (player instanceof Player) {
                        cir.setReturnValue(InteractionResult.SUCCESS);
                    }
                }
            }
        }
    }
}
