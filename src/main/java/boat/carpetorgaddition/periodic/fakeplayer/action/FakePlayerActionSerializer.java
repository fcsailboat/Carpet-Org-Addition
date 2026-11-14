package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.periodic.FakePlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class FakePlayerActionSerializer {
    private final AbstractPlayerAction action;
    private final MinecraftServer server;

    public FakePlayerActionSerializer(MinecraftServer server) {
        this.action = new StopAction(server);
        this.server = server;
    }

    public FakePlayerActionSerializer(EntityPlayerMPFake fakePlayer) {
        FakePlayerComponentCoordinator coordinator = FakePlayerComponentCoordinator.of(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        this.action = actionManager.getAction();
        this.server = ServerUtils.getServer(fakePlayer);
    }

    public FakePlayerActionSerializer(MinecraftServer server, JsonObject json) {
        this.server = server;
        for (ActionSerializeType value : ActionSerializeType.values()) {
            String serializedName = value.getSerializedName();
            if (json.has(serializedName)) {
                JsonObject actionJson = json.getAsJsonObject(serializedName);
                AbstractPlayerAction deserialize = value.deserialize(server, actionJson);
                if (deserialize.isValid()) {
                    this.action = deserialize;
                    return;
                }
                break;
            }
        }
        this.action = new StopAction(server);
    }

    /**
     * 让假玩家开始执行动作
     */
    public void startAction(@NonNull EntityPlayerMPFake fakePlayer) {
        if (this.action.isStop()) {
            return;
        }
        if (this.action.equalFakePlayer(null)) {
            this.action.setFakePlayer(fakePlayer);
        } else if (!this.action.equalFakePlayer(fakePlayer)) {
            throw new IllegalArgumentException();
        }
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.of(fakePlayer);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        actionManager.setAction(this.action);
    }

    public void clearPlayer() {
        this.action.clearFakePlayer();
    }

    public boolean hasAction() {
        return !this.action.isStop();
    }

    public Component getDisplayName() {
        return this.action.getDisplayName();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.action.isHidden() && !CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            StopAction stopAction = new StopAction(this.server);
            json.add(stopAction.getActionSerializeType().getSerializedName(), stopAction.toJson());
        } else {
            json.add(this.action.getActionSerializeType().getSerializedName(), this.action.toJson());
        }
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FakePlayerActionSerializer that = (FakePlayerActionSerializer) o;
        return Objects.equals(this.action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.action);
    }
}
