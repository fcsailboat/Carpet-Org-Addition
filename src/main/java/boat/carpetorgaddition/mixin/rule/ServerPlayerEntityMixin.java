package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {
    @Shadow
    public abstract ServerLevel level();

    @Unique
    private final ServerPlayer thisPlayer = (ServerPlayer) (Object) this;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        // 强制补货
        if (CarpetOrgAdditionSettings.FORCE_RESTOCK.value()) {
            Inventory inventory = thisPlayer.getInventory();
            restock(inventory);
        }
        // 自动同步玩家状态
        if (CarpetOrgAdditionSettings.AUTO_SYNC_PLAYER_STATUS.value() && ServerUtils.getWorld(thisPlayer).getGameTime() % 30 == 0) {
            ServerUtils.getServer(thisPlayer).getPlayerList().sendAllPlayerInfo(thisPlayer);
            BlockPos blockPos = thisPlayer.blockPosition();
            int range = (int) Math.min(thisPlayer.blockInteractionRange() + 1, 8);
            BlockPosTraverser traverser = new BlockPosTraverser(blockPos.offset(-range, -range, -range), blockPos.offset(range, range, range));
            for (BlockPos pos : traverser) {
                if (ServerUtils.getBlockCenter(blockPos).distanceTo(ServerUtils.getBlockCenter(pos)) > range) {
                    continue;
                }
                thisPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, this.level().getBlockState(pos)));
            }
        }
    }

    @Unique
    private void restock(Inventory inventory) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            // 获取快捷栏物品
            ItemStack hotbarStack = inventory.getNonEquipmentItems().get(i);
            int count = hotbarStack.getMaxStackSize() - hotbarStack.getCount();
            // 检查该物品是否可堆叠或堆叠已满
            if (hotbarStack.isStackable() && count > 0) {
                for (int j = Inventory.getSelectionSize(); j < inventory.getNonEquipmentItems().size(); j++) {
                    ItemStack inventoryStack = inventory.getItem(j);
                    // 如果可堆叠就自动补货
                    if (ItemStack.isSameItemSameComponents(hotbarStack, inventoryStack)) {
                        int inventoryStackCount = inventoryStack.getCount();
                        if (inventoryStackCount >= count) {
                            hotbarStack.grow(count);
                            inventoryStack.shrink(count);
                        } else {
                            hotbarStack.grow(inventoryStackCount);
                            inventoryStack.shrink(inventoryStackCount);
                        }
                        // 物品是否已经补满
                        count = hotbarStack.getMaxStackSize() - hotbarStack.getCount();
                        if (count <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
