package boat.carpetorgaddition.mixin.rule.fakeplayerautorestock;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @WrapMethod(method = "useItem")
    private InteractionResult useItem(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, Operation<InteractionResult> original) {
        ItemStack copy = itemStack.copy();
        InteractionResult result = original.call(player, level, itemStack, hand);
        this.restock(copy, itemStack, hand, result);
        return result;
    }

    @WrapMethod(method = "useItemOn")
    private InteractionResult useItemOn(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, BlockHitResult hitResult, Operation<InteractionResult> original) {
        ItemStack copy = itemStack.copy();
        InteractionResult result = original.call(player, level, itemStack, hand, hitResult);
        this.restock(copy, itemStack, hand, result);
        return result;
    }

    @Unique
    private void restock(ItemStack copy, ItemStack itemStack, InteractionHand hand, InteractionResult result) {
        if (FakePlayerActionManager.IN_ACTION.orElse(false)) {
            // 玩家动作的补货，交给相应的动作自行处理
            return;
        }
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_AUTO_RESTOCK.value() && this.player instanceof EntityPlayerMPFake && result.consumesAction()) {
            PlayerStorageInventory inventory = PlayerStorageInventory.of(this.player);
            if (InventoryUtils.isFragileWithMending(itemStack)) {
                inventory.replenish(hand, stack -> stack.is(itemStack.getItem()) && !InventoryUtils.isFragileWithMending(stack));
            } else if (itemStack.isEmpty()) {
                inventory.replenish(hand, stack -> InventoryUtils.canMerge(copy, stack));
            } else {
                inventory.replenish(hand, Math.max(1, itemStack.getMaxStackSize() / 2));
            }
        }
    }
}
