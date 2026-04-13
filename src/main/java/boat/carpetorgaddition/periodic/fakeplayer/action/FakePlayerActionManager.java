package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.exception.DebugTriggerException;
import boat.carpetorgaddition.periodic.FakePlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.patches.EntityPlayerMPFake;
import org.jspecify.annotations.NonNull;

public class FakePlayerActionManager {
    private final EntityPlayerMPFake fakePlayer;
    @NonNull
    private AbstractPlayerAction action;
    /**
     * 调试用途，用于手动抛出异常
     */
    private String debugExceptionMessage = null;

    public FakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
        this.action = new StopAction(this.fakePlayer);
    }

    public void tick() {
        try {
            if (CarpetOrgAddition.isDebugMode()) {
                String message = this.debugExceptionMessage;
                if (message != null) {
                    this.debugExceptionMessage = null;
                    throw new DebugTriggerException(message);
                }
            }
            // 根据假玩家动作类型执行动作
            this.action.execute();
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error(
                    "{} encountered an unexpected error while executing '{}': ",
                    PlayerUtils.getName(this.fakePlayer),
                    this.getAction().getClass().getSimpleName(),
                    e
            );
            MessageUtils.sendErrorMessage(
                    ServerUtils.getServer(this.fakePlayer),
                    PlayerActionCommand.KEY.then("error")
                            .translate(this.fakePlayer.getDisplayName(), this.getAction().getDisplayName()),
                    e
            );
            // 让假玩家停止当前操作
            this.stop();
        }
    }

    // 从另一个玩家浅拷贝此动作管理器
    public void setActionFromOldPlayer(EntityPlayerMPFake oldPlayer) {
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(oldPlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        this.setAction(actionManager.getAction());
        this.action.setFakePlayer(this.fakePlayer);
    }

    @NonNull
    public AbstractPlayerAction getAction() {
        return this.action;
    }

    public void setDebugExceptionMessage(String message) {
        this.debugExceptionMessage = message;
    }

    public void setAction(@NonNull AbstractPlayerAction action) {
        this.action.onStop();
        this.action = action;
    }

    public void stop() {
        this.setAction(new StopAction(this.fakePlayer));
    }
}
