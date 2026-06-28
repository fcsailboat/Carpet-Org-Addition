package boat.carpetorgaddition.periodic;

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
