package org.carpetorgaddition.mixin.rule;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.CustomRuleControls;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin {
    @Shadow
    public abstract HungerManager getHungerManager();

    @Shadow
    public abstract boolean isSpectator();

    @Shadow
    protected abstract void collideWithEntity(Entity entity);

    @Unique
    private final PlayerEntity thisPlayer = (PlayerEntity) (Object) this;

    // 血量不满时也可以进食
    @Inject(method = "canConsume", at = @At("HEAD"), cancellable = true)
    private void canEat(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.healthNotFullCanEat.get() && thisPlayer.getHealth() < thisPlayer.getMaxHealth() - 0.3 // -0.3：可能生命值不满但是显示的心满了
            && this.getHungerManager().getSaturationLevel() <= 5) {
            cir.setReturnValue(true);
        }
    }

    // 快速设置假玩家合成
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void interact(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (this.isSpectator()) {
            return;
        }
        switch (CarpetOrgAdditionSettings.quickSettingFakePlayerCraft.get()) {
            case FALSE:
                break;
            case SNEAKING:
                if (!thisPlayer.isSneaking()) {
                    break;
                }
            case TRUE:
                if (openQuickCraftGui(entity)) {
                    cir.setReturnValue(ActionResult.SUCCESS);
                }
            default: {
            }
        }
    }

    /**
     * 打开合成配方设置GUI
     *
     * @return 是否执行成功
     */
    @Unique
    private boolean openQuickCraftGui(Entity entity) {
        Optional<Function<EntityPlayerMPFake, String>> optional = getOpenQuickCraftGuiCommand(thisPlayer.getMainHandStack());
        if (optional.isEmpty()) {
            return false;
        }
        if (thisPlayer instanceof ServerPlayerEntity player && entity instanceof EntityPlayerMPFake fakePlayer) {
            CommandUtils.execute(player, optional.get().apply(fakePlayer));
        }
        // 客户端虽然不执行命令，但仍要返回true，否则不会显示摆动手动画
        return true;
    }

    // 获取打开GUI所需要的命令
    @Unique
    private Optional<Function<EntityPlayerMPFake, String>> getOpenQuickCraftGuiCommand(ItemStack itemStack) {
        PermissionPredicate predicate = thisPlayer.getPermissions();
        boolean canUseCommand = CommandUtils.canUseCommand(predicate, CarpetOrgAdditionSettings.commandPlayerAction.get());
        if (canUseCommand) {
            if (itemStack.isEmpty()) {
                return Optional.empty();
            }
            // 工作台
            if (itemStack.isOf(Items.CRAFTING_TABLE)) {
                return Optional.of(CommandProvider::openPlayerCraftGui);
            }
            // 切石机
            if (itemStack.isOf(Items.STONECUTTER)) {
                return Optional.of(CommandProvider::openPlayerStonecuttingGui);
            }
        }
        return Optional.empty();
    }

    // 最大方块交互距离
    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (FetcherUtils.getWorld(thisPlayer).isClient() && !CarpetOrgAdditionSettings.maxBlockPlaceDistanceSyncClient.get()) {
            return;
        }
        if (RuleUtils.isDefaultDistance()) {
            return;
        }
        cir.setReturnValue(RuleUtils.getPlayerMaxInteractionDistance());
    }

    // 实体交互距离
    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (CarpetOrgAdditionSettings.maxBlockPlaceDistanceReferToEntity.get()) {
            cir.setReturnValue(RuleUtils.getPlayerMaxInteractionDistance());
        }
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At(value = "HEAD"))
    private void getBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        if (CarpetOrgAdditionSettings.applyToolEffectsImmediately.get()) {
            this.onPlayerBreakBlock();
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private void pickupItem(CallbackInfo ci, @Local Box box) {
        if (this.thisPlayer instanceof ServerPlayerEntity player) {
            int range = CustomRuleControls.ITEM_PICKUP_RANGE_EXPAND.getRuleValue(player);
            if (range == 0) {
                return;
            }
            double minX = box.minX - range;
            double minY = box.minY - range;
            double minZ = box.minZ - range;
            double maxX = box.maxX + range;
            double maxY = box.maxY + range;
            double maxZ = box.maxZ + range;
            Box expand = new Box(minX, minY, minZ, maxX, maxY, maxZ);
            List<Entity> list = FetcherUtils.getWorld(this.thisPlayer)
                    .getOtherEntities(this.thisPlayer, expand)
                    .stream()
                    .filter(entity -> !entity.isRemoved())
                    .filter(entity -> entity.getType() == EntityType.ITEM)
                    .toList();
            for (Entity entity : list) {
                this.collideWithEntity(entity);
            }
        }
    }
}
