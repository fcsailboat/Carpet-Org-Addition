package org.carpetorgaddition.mixin.util.carpet;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.InvalidRuleValueException;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAdditionExtension;
import org.carpetorgaddition.dataupdate.CarpetConfDataUpdater;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.rule.OrgRule;
import org.carpetorgaddition.rule.RuleConfig;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Mixin(value = SettingsManager.class, remap = false)
public abstract class SettingsManagerMixin {
    @Shadow
    @Final
    private Map<String, CarpetRule<?>> rules;

    @Shadow
    private MinecraftServer server;

    @Shadow
    public abstract boolean locked();

    @Shadow
    protected abstract Path getFile();

    @Unique
    private final SettingsManager thisManager = (SettingsManager) (Object) this;

    @Inject(method = "setDefault", at = @At(value = "HEAD"), cancellable = true)
    private void setDefault(ServerCommandSource source, CarpetRule<?> rule, String stringValue, CallbackInfoReturnable<Integer> cir) {
        if (rule instanceof OrgRule<?> && thisManager == CarpetOrgAdditionExtension.getSettingManager()) {
            if (locked() || !this.rules.containsKey(rule.name())) {
                cir.setReturnValue(0);
                return;
            }
            try {
                rule.set(source, stringValue);
            } catch (InvalidRuleValueException e) {
                e.notifySource(rule.name(), source);
                // 先修改规则值，再写入文件：https://github.com/gnembon/fabric-carpet/issues/2004
                cir.setReturnValue(0);
                return;
            }
            RuleConfig ruleConfig = ServerComponentCoordinator.getCoordinator(this.server).getRuleConfig();
            // 保存规则到配置文件
            ruleConfig.put(rule, stringValue);
            TextBuilder builder = TextBuilder.of("carpet.settings.command.default_set", RuleUtils.simpleTranslationName(rule), stringValue);
            builder.setGrayItalic();
            MessageUtils.sendMessage(source, builder.build());
            cir.setReturnValue(1);
        }
    }

    @Inject(method = "removeDefault", at = @At("HEAD"), cancellable = true)
    private void removeDefault(ServerCommandSource source, CarpetRule<?> rule, CallbackInfoReturnable<Integer> cir) {
        if (rule instanceof OrgRule<?> && thisManager == CarpetOrgAdditionExtension.getSettingManager()) {
            if (locked() || !this.rules.containsKey(rule.name())) {
                cir.setReturnValue(0);
                return;
            }
            RuleConfig ruleConfig = ServerComponentCoordinator.getCoordinator(this.server).getRuleConfig();
            ruleConfig.remove(rule);
            // 将规则设置为默认值
            RuleHelper.resetToDefault(rule, source);
            TextBuilder builder = TextBuilder.of("carpet.settings.command.default_removed", RuleUtils.simpleTranslationName(rule));
            builder.setGrayItalic();
            MessageUtils.sendMessage(source, builder.build());
            cir.setReturnValue(1);
        }
    }

    @WrapOperation(method = "loadConfigurationFromConf", at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;"))
    private Set<String> migrate(Map<String, String> map, Operation<Set<String>> original) {
        if (thisManager == CarpetOrgAdditionExtension.getSettingManager()) {
            RuleConfig ruleConfig = ServerComponentCoordinator.getCoordinator(this.server).getRuleConfig();
            if (ruleConfig.isMigrated()) {
                ruleConfig.load();
                return original.call(map);
            }
            JsonObject json = new JsonObject();
            HashSet<String> set = new HashSet<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (CarpetConfDataUpdater.OLD_VERSION_RULES.contains(entry.getKey())) {
                    json.addProperty(entry.getKey(), entry.getValue());
                } else {
                    set.add(entry.getKey());
                }
            }
            // 迁移规则
            ruleConfig.migrate(json);
            ruleConfig.load();
            File file = this.getFile().toFile();
            if (file.isFile()) {
                // 从carpet.conf中删除来自org的规则
                this.removeRulesFromCarpetConf(file);
            }
            return set;
        }
        return original.call(map);
    }

    @WrapOperation(method = "readSettingsFromConf", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"))
    private boolean readSettingsFromConf(Map<String, String> instance, Object o, Operation<Boolean> original) {
        if (thisManager == CarpetOrgAdditionExtension.getSettingManager()) {
            RuleConfig ruleConfig = ServerComponentCoordinator.getCoordinator(this.server).getRuleConfig();
            if (ruleConfig.isMigrated()) {
                return original.call(instance, o);
            }
            if (o instanceof String && CarpetConfDataUpdater.OLD_VERSION_RULES.contains(o)) {
                return true;
            }
        }
        return original.call(instance, o);
    }

    @Unique
    private void removeRulesFromCarpetConf(File file) {
        IOUtils.backupFile(file);
        // 从carpet.conf删除Carpet Org Addition的规则
        ArrayList<String> list = new ArrayList<>();
        try {
            BufferedReader reader = IOUtils.toReader(file);
            try (reader) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isCarpetOrgAdditionRule(line)) {
                        continue;
                    }
                    list.add(line);
                }
            }
            StringJoiner joiner = new StringJoiner("\n");
            for (String rule : list) {
                joiner.add(rule);
            }
            IOUtils.write(file, joiner.toString());
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
    }

    @Unique
    private boolean isCarpetOrgAdditionRule(String line) {
        String[] split = line.replaceAll("[\\r\\n]", "").split("\\s");
        if (split.length <= 1 || split[0].startsWith("#") || split[1].startsWith("#")) {
            return false;
        }
        return CarpetConfDataUpdater.OLD_VERSION_RULES.contains(split[0]);
    }
}
