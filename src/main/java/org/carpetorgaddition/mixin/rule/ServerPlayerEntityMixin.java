package org.carpetorgaddition.mixin.rule;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.wheel.SelectionArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("SpellCheckingInspection")
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Shadow
    public abstract ServerWorld getServerWorld();

    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;

    // 玩家被闪电苦力怕炸死掉落头颅
    @Inject(method = "onDeath", at = @At("TAIL"))
    private void dropHead(DamageSource damageSource, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.playerDropHead
                && damageSource.getAttacker() instanceof CreeperEntity creeperEntity
                && creeperEntity.shouldDropHead()) {
            ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
            itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(thisPlayer.getGameProfile()));
            creeperEntity.onHeadDropped();
            thisPlayer.dropStack(thisPlayer.getServerWorld(), itemStack);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        // 强制补货
        if (CarpetOrgAdditionSettings.forceRestock) {
            PlayerInventory inventory = thisPlayer.getInventory();
            restock(inventory);
        }
        // 自动同步玩家状态
        if (CarpetOrgAdditionSettings.autoSyncPlayerStatus && thisPlayer.getWorld().getTime() % 30 == 0) {
            thisPlayer.server.getPlayerManager().sendPlayerStatus(thisPlayer);
            BlockPos blockPos = thisPlayer.getBlockPos();
            int range = (int) Math.min(thisPlayer.getBlockInteractionRange() + 1, 8);
            SelectionArea selectionArea = new SelectionArea(blockPos.add(-range, -range, -range), blockPos.add(range, range, range));
            for (BlockPos pos : selectionArea) {
                if (blockPos.toCenterPos().distanceTo(pos.toCenterPos()) > range) {
                    continue;
                }
                thisPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, this.getServerWorld().getBlockState(pos)));
            }
        }
    }

    @Unique
    private void restock(PlayerInventory inventory) {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            // 获取快捷栏物品
            ItemStack hotbarStack = inventory.main.get(i);
            int count = hotbarStack.getMaxCount() - hotbarStack.getCount();
            // 检查该物品是否可堆叠或堆叠已满
            if (hotbarStack.isStackable() && count > 0) {
                for (int j = PlayerInventory.getHotbarSize(); j < inventory.main.size(); j++) {
                    ItemStack inventoryStack = inventory.getStack(j);
                    // 如果可堆叠就自动补货
                    if (ItemStack.areItemsAndComponentsEqual(hotbarStack, inventoryStack)) {
                        int inventoryStackCount = inventoryStack.getCount();
                        if (inventoryStackCount >= count) {
                            hotbarStack.increment(count);
                            inventoryStack.decrement(count);
                        } else {
                            hotbarStack.increment(inventoryStackCount);
                            inventoryStack.decrement(inventoryStackCount);
                        }
                        // 物品是否已经补满
                        count = hotbarStack.getMaxCount() - hotbarStack.getCount();
                        if (count <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
