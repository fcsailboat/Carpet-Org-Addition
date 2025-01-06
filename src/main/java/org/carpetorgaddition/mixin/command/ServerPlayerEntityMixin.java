package org.carpetorgaddition.mixin.command;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSafeAfkInterface;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.matcher.ItemMatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Optional;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements FakePlayerSafeAfkInterface {
    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;

    @Unique
    private float safeAfkThreshold = -1F;

    @Inject(method = "damage", at = @At(value = "RETURN"))
    private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.safeAfkThreshold > 0 && thisPlayer instanceof EntityPlayerMPFake) {
            safeAfk(source, amount);
        }
    }

    // 假玩家安全挂机
    @Unique
    private void safeAfk(DamageSource source, float amount) {
        // 检查玩家是否可以触发不死图腾
        if (this.canTriggerTotemOfUndying(source)) {
            return;
        }
        // 安全挂机触发失败，玩家已死亡
        if (this.afkTriggerFail()) {
            MutableText message = TextUtils.translate("carpet.commands.playerManager.safeafk.trigger.fail", thisPlayer.getDisplayName());
            // 设置为斜体
            message = TextUtils.toItalic(message);
            // 设置为红色
            message = TextUtils.setColor(message, Formatting.RED);
            // 添加悬停提示
            message = TextUtils.hoverText(message, report(source, amount));
            MessageUtils.broadcastMessage(thisPlayer.server, message);
            return;
        }
        // 玩家安全挂机触发成功
        if (thisPlayer.getHealth() <= this.safeAfkThreshold) {
            // 假玩家剩余血量
            String health = MathUtils.numberToTwoDecimalString(thisPlayer.getHealth());
            MutableText message = TextUtils.translate("carpet.commands.playerManager.safeafk.trigger.success",
                    thisPlayer.getDisplayName(), health);
            // 添加悬停提示
            message = TextUtils.hoverText(message, report(source, amount));
            // 广播触发消息，斜体淡灰色
            MessageUtils.broadcastMessage(thisPlayer.server, TextUtils.toGrayItalic(message));
            // 恢复饥饿值
            thisPlayer.getHungerManager().setFoodLevel(20);
            // 退出假人
            thisPlayer.kill();
        }
    }

    // 反馈中的悬停提示
    @Unique
    private Text report(DamageSource damageSource, float amount) {
        ArrayList<Text> list = new ArrayList<>();
        // 获取攻击者
        Object attacker = Optional.ofNullable(damageSource.getAttacker()).map(entity -> (Object) entity.getDisplayName()).orElse("null");
        // 获取伤害来源
        Object source = Optional.ofNullable(damageSource.getSource()).map(entity -> (Object) entity.getDisplayName()).orElse("null");
        list.add(TextUtils.translate("carpet.commands.playerManager.safeafk.info.attacker", attacker));
        list.add(TextUtils.translate("carpet.commands.playerManager.safeafk.info.source", source));
        list.add(TextUtils.translate("carpet.commands.playerManager.safeafk.info.type", damageSource.getName()));
        list.add(TextUtils.translate("carpet.commands.playerManager.safeafk.info.amount", String.valueOf(amount)));
        return TextUtils.appendList(list);
    }

    // 假玩家是否可以触发图腾
    @Unique
    private boolean canTriggerTotemOfUndying(DamageSource source) {
        // 无法触发不死图腾的伤害类型
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        switch (CarpetOrgAdditionSettings.betterTotemOfUndying) {
            case FALSE: {
                // 主手或副手有不死图腾
                if (thisPlayer.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    return true;
                }
                if (thisPlayer.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    return true;
                }
                break;
            }
            case SHULKER_BOX: {
                // 检查潜影盒中是否有不死图腾
                PlayerInventory inventory = thisPlayer.getInventory();
                for (int i = 0; i < inventory.size(); i++) {
                    ItemStack itemStack = inventory.getStack(i);
                    if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                        MutableBoolean bool = new MutableBoolean(false);
                        ItemMatcher matcher = new ItemMatcher(Items.TOTEM_OF_UNDYING);
                        InventoryUtils.shulkerBoxConsumer(itemStack, matcher, (stack) -> bool.setTrue());
                        if (bool.getValue()) {
                            return true;
                        }
                    }
                }
            }
            case TRUE: {
                // 物品栏中有不死图腾
                PlayerInventory inventory = thisPlayer.getInventory();
                for (int i = 0; i < inventory.size(); i++) {
                    if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                        return true;
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException();
        }
        return false;
    }

    @Override
    public void setHealthThreshold(float threshold) {
        this.safeAfkThreshold = threshold;
    }

    @Override
    public float getHealthThreshold() {
        return this.safeAfkThreshold;
    }

    @Override
    public boolean afkTriggerFail() {
        return false;
    }
}
