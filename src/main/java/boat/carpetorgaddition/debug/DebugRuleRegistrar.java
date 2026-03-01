package boat.carpetorgaddition.debug;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.exception.ProductionEnvironmentError;
import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;

import java.util.HashMap;
import java.util.Map;

public class DebugRuleRegistrar implements CarpetExtension {
    private final SettingsManager settingsManager;
    private static DebugRuleRegistrar instance;
    private static final HashMap<String, String> TRANSLATIONS = new HashMap<>();

    public static DebugRuleRegistrar getInstance() {
        if (instance == null) {
            instance = new DebugRuleRegistrar();
        }
        return instance;
    }

    private DebugRuleRegistrar() {
        ProductionEnvironmentError.assertDevelopmentEnvironment();
        this.settingsManager = new SettingsManager(
                CarpetOrgAdditionConstants.VERSION,
                "carpet-debug",
                "Carpet Org Addition Debug"
        );
    }

    public void registrar() {
        CarpetServer.manageExtension(this);
    }

    @Override
    public SettingsManager extensionSettingsManager() {
        return this.settingsManager;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return TRANSLATIONS;
    }
}
