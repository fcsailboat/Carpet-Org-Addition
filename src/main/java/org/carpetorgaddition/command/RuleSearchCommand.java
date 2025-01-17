package org.carpetorgaddition.command;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.mixin.rule.carpet.SettingsManagerAccessor;
import org.carpetorgaddition.util.TextUtils;

import java.util.Collection;

public class RuleSearchCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ruleSearch")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandRuleSearch))
                .then(CommandManager.argument("rule", StringArgumentType.greedyString())
                        .executes(RuleSearchCommand::listRule)));
    }

    // 列出符合条件的规则
    private static int listRule(CommandContext<ServerCommandSource> context) {
        String rule = StringArgumentType.getString(context, "rule");
        if (rule.matches("\".*\"")) {
            rule = rule.substring(1, rule.length() - 1);
        }
        if (CarpetServer.settingsManager == null) {
            return 0;
        }
        MutableText text = TextUtils.translate("carpet.commands.ruleSearch.feedback", rule);
        // 将文本设置为粗体
        text.styled(style -> style.withBold(true));
        context.getSource().sendFeedback(() -> text, true);
        // 如果字符串为空，不搜索规则
        if (rule.isEmpty()) {
            return 0;
        }
        return listRule(context, rule);
    }

    private static int listRule(CommandContext<ServerCommandSource> context, String rule) {
        MutableInt ruleCount = new MutableInt(0);
        CarpetServer.forEachManager(settingsManager -> {
            SettingsManagerAccessor accessor = (SettingsManagerAccessor) settingsManager;
            Collection<CarpetRule<?>> rules = settingsManager.getCarpetRules();
            for (CarpetRule<?> carpetRule : rules) {
                if (RuleHelper.translatedName(carpetRule).contains(rule)) {
                    Messenger.m(context.getSource(), accessor.displayInteractiveSettings(carpetRule));
                    ruleCount.add(1);
                }
            }
        });
        return ruleCount.getValue();
    }
}
