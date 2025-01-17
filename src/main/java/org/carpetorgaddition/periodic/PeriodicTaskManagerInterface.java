package org.carpetorgaddition.periodic;

public interface PeriodicTaskManagerInterface {
    default ServerPeriodicTaskManager carpet_Org_Addition$getServerPeriodicTaskManager() {
        throw new UnsupportedOperationException();
    }

    default PlayerPeriodicTaskManager carpet_Org_Addition$getPlayerPeriodicTaskManager() {
        throw new UnsupportedOperationException();
    }
}
