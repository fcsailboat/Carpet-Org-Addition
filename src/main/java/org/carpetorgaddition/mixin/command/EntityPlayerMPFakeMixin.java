package org.carpetorgaddition.mixin.command;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.fakeplayer.FakePlayerActionInterface;
import org.carpetorgaddition.util.fakeplayer.FakePlayerActionManager;
import org.carpetorgaddition.util.fakeplayer.FakePlayerSafeAfkInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(EntityPlayerMPFake.class)
public abstract class EntityPlayerMPFakeMixin implements FakePlayerActionInterface {
    @Unique
    private final EntityPlayerMPFake thisPlayer = (EntityPlayerMPFake) (Object) this;

    @Unique
    private final FakePlayerActionManager actionManager = new FakePlayerActionManager(thisPlayer);

    @Unique
    private boolean isDead = false;

    @Override
    public FakePlayerActionManager getActionManager() {
        return this.actionManager;
    }

    @Override
    public void copyActionManager(EntityPlayerMPFake oldPlayer) {
        this.actionManager.copyActionData(oldPlayer);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fakePlayerTick(CallbackInfo ci) {
        try {
            // 根据假玩家动作类型执行动作
            this.getActionManager().executeAction();
        } catch (RuntimeException e) {
            // 将错误信息写入日志
            CarpetOrgAddition.LOGGER.error("{}在执行操作“{}”时遇到意外错误:", thisPlayer.getName().getString(),
                    this.getActionManager().getAction().toString(), e);
            // 向聊天栏发送错误消息的反馈
            MutableText message = TextUtils.translate("carpet.commands.playerAction.exception.runtime",
                    thisPlayer.getDisplayName(), this.getActionManager().getAction().getDisplayName());
            MutableText errorMessage = TextUtils.hoverText(TextUtils.setColor(message, Formatting.RED), e.getMessage());
            MessageUtils.broadcastMessage(thisPlayer, errorMessage);
            // 让假玩家停止当前操作
            this.getActionManager().stop();
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource cause, CallbackInfo ci) {
        this.isDead = true;
    }

    /**
     * @apiNote 尽管这个方法没有 {@code @Override} 注解，但这这不妨碍它重写了接口{@link FakePlayerSafeAfkInterface}中的
     * {@code afkTriggerFail()}方法，该接口在父类的Mixin类中被实现
     */
    @SuppressWarnings({"MissingUnique", "unused"})
    public boolean afkTriggerFail() {
        return isDead;
    }
}
