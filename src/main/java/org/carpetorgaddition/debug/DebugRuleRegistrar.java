package org.carpetorgaddition.debug;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import carpet.utils.Translations;
import net.fabricmc.loader.api.FabricLoader;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.ProductionEnvironmentError;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DebugRuleRegistrar implements CarpetExtension {
    private final SettingsManager settingsManager;

    private static DebugRuleRegistrar instance;

    public static final HashMap<String, String> TRANSLATIONS = new HashMap<>();

    public static DebugRuleRegistrar getInstance() {
        if (instance == null) {
            instance = new DebugRuleRegistrar();
        }
        return instance;
    }

    private DebugRuleRegistrar() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            this.settingsManager = new SettingsManager(
                    FabricLoader.getInstance().getModContainer(CarpetOrgAddition.MOD_ID).orElseThrow().toString(),
                    CarpetOrgAddition.MOD_ID,
                    "CarpetOrgAdditionDebug"
            );
        } else {
            throw new ProductionEnvironmentError();
        }
    }

    public void registrar() {
        CarpetServer.manageExtension(this);
        Field[] fields = DebugSettings.class.getDeclaredFields();
        for (Field field : fields) {
            DebugRule debugRule = field.getAnnotation(DebugRule.class);
            if (debugRule == null) {
                continue;
            }
            this.parse(field, debugRule);
        }
        Translations.updateLanguage();
    }

    @SuppressWarnings("rawtypes")
    private void parse(Field field, DebugRule debugRule) {
        try {
            translation(field, debugRule);
            // 使用全类名获取字节码是为了避免编译器警告
            Class<?> parsedRuleClass = Class.forName("carpet.settings.ParsedRule");
            Class<?> ruleAnnotationClass = Class.forName("carpet.settings.ParsedRule$RuleAnnotation");
            Constructor<?> ruleAnnotationConstructor = ruleAnnotationClass.getDeclaredConstructor(
                    boolean.class,
                    String.class,
                    String.class,
                    String[].class,
                    String[].class,
                    String[].class,
                    boolean.class,
                    String.class,
                    Class[].class
            );
            ruleAnnotationConstructor.setAccessible(true);
            Object ruleAnnotation = ruleAnnotationConstructor.newInstance(
                    true,
                    field.getName(),
                    debugRule.desc(),
                    debugRule.extra(),
                    new String[]{CarpetOrgAdditionSettings.ORG, "调试"},
                    debugRule.options(),
                    false,
                    "",
                    new Class[0]
            );
            Constructor<?> parsedRuleConstructor = parsedRuleClass.getDeclaredConstructor(
                    Field.class,
                    ruleAnnotationClass,
                    SettingsManager.class
            );
            parsedRuleConstructor.setAccessible(true);
            CarpetRule parsedRule = (CarpetRule) parsedRuleConstructor.newInstance(field, ruleAnnotation, settingsManager);
            settingsManager.addCarpetRule(parsedRule);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void translation(Field field, DebugRule debugRule) {
        String nameKey = "%s.rule.%s.name".formatted(this.settingsManager.identifier(), field.getName());
        String descKey = "%s.rule.%s.desc".formatted(this.settingsManager.identifier(), field.getName());
        TRANSLATIONS.put(nameKey, debugRule.name());
        TRANSLATIONS.put(descKey, debugRule.desc());
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
