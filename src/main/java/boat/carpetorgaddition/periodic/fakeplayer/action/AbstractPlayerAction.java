package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.periodic.FakePlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractPlayerAction {
    @Nullable
    private EntityPlayerMPFake fakePlayer;
    private boolean isPlayerChanged = false;

    public AbstractPlayerAction(@Nullable EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
        if (this.fakePlayer != null) {
            this.isPlayerChanged = true;
        }
    }

    public final void execute() {
        if (this.isPlayerChanged) {
            this.onAssignPlayer();
            this.isPlayerChanged = false;
        }
        Objects.requireNonNull(this.fakePlayer);
        if (this.isValid()) {
            this.tick();
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * 每个游戏刻都执行
     */
    protected abstract void tick();

    /**
     * 当前动作的详细信息
     */
    public abstract List<Component> info();

    /**
     * 序列化假玩家动作数据
     */
    public abstract JsonObject toJson();

    /**
     * @return 当前动作类型的显示名称
     */
    public Component getDisplayName() {
        return this.getLocalizationKey().translate();
    }

    protected abstract LocalizationKey getLocalizationKey();

    protected LocalizationKey getInfoLocalizationKey() {
        return this.getLocalizationKey().then("info");
    }

    public abstract ActionSerializeType getActionSerializeType();

    /**
     * 当前动作是否是隐藏的
     */
    public boolean isHidden() {
        return false;
    }

    public boolean isValid() {
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            return true;
        }
        return !this.isHidden();
    }

    public boolean isStop() {
        return false;
    }

    /**
     * 当玩家停止当前动作时调用
     */
    public void onStop() {
    }

    public void stop() {
        if (this.fakePlayer == null) {
            return;
        }
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(this.fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        actionManager.setAction(new StopAction(this.fakePlayer));
    }

    @NonNull
    @Contract(pure = true)
    protected EntityPlayerMPFake getFakePlayer() {
        return Objects.requireNonNull(this.fakePlayer);
    }

    protected Optional<EntityPlayerMPFake> getFakePlayerNullable() {
        return Optional.ofNullable(this.fakePlayer);
    }

    public boolean equalFakePlayer(@Nullable EntityPlayerMPFake fakePlayer) {
        return Objects.equals(this.fakePlayer, fakePlayer);
    }

    @Contract("null -> fail")
    public void setFakePlayer(EntityPlayerMPFake fakePlayer) {
        if (fakePlayer == null) {
            throw new IllegalArgumentException();
        }
        this.fakePlayer = fakePlayer;
        this.onAssignPlayer();
    }

    protected MinecraftServer getServer() {
        return ServerUtils.getServer(this.getFakePlayer());
    }

    public void clearFakePlayer() {
        this.fakePlayer = null;
        this.onClearPlayer();
    }

    /**
     * 当玩家被赋值时调用
     */
    protected void onAssignPlayer() {
    }

    /**
     * 当玩家被赋值为{@code null}时调用
     */
    protected void onClearPlayer() {
    }

    /**
     * 当玩家退出游戏时调用，用于提前关闭GUI
     */
    public void onFakePlayerLogout() {
        // 假玩家退出游戏时，会先将玩家实体标记为已删除再关闭GUI，这会导致GUI中存放的物品无法回到玩家物品栏
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
}
