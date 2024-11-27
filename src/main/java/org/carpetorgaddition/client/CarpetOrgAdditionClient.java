package org.carpetorgaddition.client;

import net.fabricmc.api.ClientModInitializer;

public class CarpetOrgAdditionClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        CarpetOrgAdditionClientRegister.register();
    }
}
