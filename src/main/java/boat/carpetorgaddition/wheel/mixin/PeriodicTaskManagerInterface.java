package boat.carpetorgaddition.wheel.mixin;

import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;

public interface PeriodicTaskManagerInterface {
    default ServerComponentCoordinator carpet_Org_Addition$getServerComponentCoordinator() {
        throw new UnsupportedOperationException();
    }

    default void carpet_Org_Addition$setServerComponentCoordinator(ServerComponentCoordinator coordinator) {
    }

    default PlayerComponentCoordinator carpet_Org_Addition$getPlayerPeriodicTaskManager() {
        throw new UnsupportedOperationException();
    }

    default void carpet_Org_Addition$setPlayerComponentCoordinator(PlayerComponentCoordinator coordinator) {
    }
}
