package boat.carpetorgaddition.mixin.rule.quickshulker;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
    private void onStackClicked(ItemStack stack, Slot slot, ClickAction clickType, Player player, CallbackInfoReturnable<Boolean> cir) {
        // 使用潜影盒单击物品
        if (CarpetOrgAdditionSettings.QUICK_SHULKER.value()) {
            if (clickType == ClickAction.PRIMARY || stack.isEmpty()) {
                return;
            }
            if (InventoryUtils.isOperableSulkerBox(stack)) {
                ItemStack slotStack = slot.getItem();
                if (slotStack.isEmpty()) {
                    ItemStack first = InventoryUtils.getFirstItemStack(stack);
                    if (slot.mayPlace(first)) {
                        // 取出潜影盒中的物品
                        ItemStack picked = InventoryUtils.pickItemFromShulkerBox(stack, ItemStackPredicate.WILDCARD);
                        ItemStack inserted = slot.safeInsert(picked);
                        if (!inserted.isEmpty()) {
                            // 物品可以被放入槽位，但是不能完全放入，例如在潜影盒中装入多个可以激活信标的物品，然后使用潜影盒单击信标槽位，
                            // 由于信标槽位中最多只能容纳1个物品，物品不能完全放入，因此这里的代码可能会被执行
                            ItemStack result = InventoryUtils.addItemToShulkerBox(stack, inserted);
                            insertToPlayerInventory(player, result);
                        }
                    }
                } else if (slotStack.getItem().canFitInsideContainerItems()) {
                    // 将物品放入潜影盒
                    int count = InventoryUtils.shulkerCanInsertItemCount(stack, slotStack);
                    ItemStack taken = slot.safeTake(slotStack.getCount(), count, player);
                    ItemStack result = InventoryUtils.addItemToShulkerBox(stack, taken);
                    insertToPlayerInventory(player, result);
                }
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private void insertToPlayerInventory(Player player, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        player.getInventory().add(result);
        if (result.isEmpty()) {
            return;
        }
        player.drop(result, false);
    }

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess reference, CallbackInfoReturnable<Boolean> cir) {
        // 使用物品单击潜影盒
        if (CarpetOrgAdditionSettings.QUICK_SHULKER.value()) {
            if (clickType == ClickAction.PRIMARY || stack.isEmpty()) {
                return;
            }
            // 要求槽位是可以取可以放的，避免玩家向工作台输出槽中的潜影盒中放入物品
            if (InventoryUtils.isOperableSulkerBox(stack) && slot.allowModification(player)) {
                ItemStack itemStack = InventoryUtils.addItemToShulkerBox(stack, reference.get());
                reference.set(itemStack);
                cir.setReturnValue(true);
            }
        }
    }
}
