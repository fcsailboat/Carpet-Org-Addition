package boat.carpetorgaddition.mixin.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.KillMeCommand;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerSafeAfkInterface;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin implements FakePlayerSafeAfkInterface {
    @Unique
    private final ServerPlayer self = (ServerPlayer) (Object) this;

    @Unique
    private float safeAfkThreshold = -1F;

    @Inject(method = "hurtServer", at = @At(value = "RETURN"))
    private void damage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (this.safeAfkThreshold > 0 && this.self instanceof EntityPlayerMPFake) {
            safeAfk(source, damage);
        }
    }

    @WrapOperation(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
    private Component getDeathMessage(CombatTracker instance, Operation<Component> original) {
        if (CarpetOrgAdditionSettings.COMMITTING_SUICIDE.orElse(false)) {
            return KillMeCommand.KEY.then("suicide").translate(this.self.getDisplayName());
        }
        return original.call(instance);
    }

    // 假玩家安全挂机
    @Unique
    private void safeAfk(DamageSource source, float amount) {
        // 检查玩家是否可以触发不死图腾
        if (this.canTriggerTotemOfUndying(source)) {
            return;
        }
        // 安全挂机触发失败，玩家已死亡
        if (this.carpet_Org_Addition$afkTriggerFail()) {
            TextBuilder builder = TextBuilder.of(KEY.then("fail").translate(this.self.getDisplayName()));
            builder.setItalic();
            builder.setColor(ChatFormatting.RED);
            builder.setHover(this.report(source, amount));
            MessageUtils.sendMessage(ServerUtils.getServer(this.self), builder.build());
            return;
        }
        // 玩家安全挂机触发成功
        if (this.self.getHealth() <= this.safeAfkThreshold) {
            // 假玩家剩余血量
            String health = MathUtils.formatToMaxTwoDecimals(this.self.getHealth());
            TextBuilder builder = TextBuilder.of(KEY.then("success").translate(this.self.getDisplayName(), health));
            builder.setHover(this.report(source, amount));
            builder.setGrayItalic();
            MessageUtils.sendMessage(ServerUtils.getServer(this.self), builder.build());
            // 恢复饥饿值
            this.self.getFoodData().setFoodLevel(20);
            this.self.kill(ServerUtils.getWorld(this.self));
        }
    }

    // 反馈中的悬停提示
    @Unique
    private Component report(DamageSource damageSource, float amount) {
        ArrayList<Component> list = new ArrayList<>();
        // 获取攻击者
        Object attacker = Optional.ofNullable(damageSource.getEntity()).map(entity -> (Object) entity.getDisplayName()).orElse("null");
        // 获取伤害来源
        Object source = Optional.ofNullable(damageSource.getDirectEntity()).map(entity -> (Object) entity.getDisplayName()).orElse("null");
        LocalizationKey key = KEY.then("info");
        list.add(key.then("attacker").translate(attacker));
        list.add(key.then("source").translate(source));
        list.add(key.then("type").translate(damageSource.getMsgId()));
        list.add(key.then("amount").translate(String.valueOf(amount)));
        return TextBuilder.joinList(list);
    }

    // 假玩家是否可以触发图腾
    @Unique
    private boolean canTriggerTotemOfUndying(DamageSource source) {
        // 无法触发不死图腾的伤害类型
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        switch (CarpetOrgAdditionSettings.BETTER_TOTEM_OF_UNDYING.value()) {
            case VANILLA: {
                // 主手或副手有不死图腾
                if (self.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
                    return true;
                }
                if (self.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                    return true;
                }
                break;
            }
            case INVENTORY_WITH_SHULKER_BOX: {
                // 检查潜影盒中是否有不死图腾
                Inventory inventory = self.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack itemStack = inventory.getItem(i);
                    if (InventoryUtils.contains(itemStack, stack -> stack.is(Items.TOTEM_OF_UNDYING))) {
                        return true;
                    }
                }
            }
            case INVENTORY: {
                // 物品栏中有不死图腾
                Inventory inventory = self.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
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
    public void carpet_Org_Addition$setHealthThreshold(float threshold) {
        this.safeAfkThreshold = threshold;
    }

    @Override
    public float carpet_Org_Addition$getHealthThreshold() {
        return this.safeAfkThreshold;
    }

    @Override
    public boolean carpet_Org_Addition$afkTriggerFail() {
        return false;
    }
}
