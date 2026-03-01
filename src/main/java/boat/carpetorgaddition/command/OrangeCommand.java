package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.rule.CustomRuleControl;
import boat.carpetorgaddition.rule.CustomRuleEntry;
import boat.carpetorgaddition.rule.CustomRuleValueManager;
import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.permission.CommandPermission;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.api.settings.CarpetRule;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

public class OrangeCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("orange");
    public static final LocalizationKey RULESELF = KEY.then("ruleself");

    public OrangeCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .then(Commands.literal("permission")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.argument("node", StringArgumentType.string())
                                .suggests(suggestsNode())
                                .then(Commands.argument("level", StringArgumentType.string())
                                        .suggests((_, builder) -> SharedSuggestionProvider.suggest(PermissionLevel.listPermission(), builder))
                                        .executes(this::setLevel))))
                .then(Commands.literal("version")
                        .executes(this::version))
                .then(Commands.literal("ruleself")
                        .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                .then(Commands.argument("rule", StringArgumentType.string())
                                        .suggests(suggestRule())
                                        .executes(this::infoRuleSelf)
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(this::setRuleSelf)))))
                .then(Commands.literal("dialog")
                        .executes(this::openDialog)));
    }

    private @NotNull SuggestionProvider<CommandSourceStack> suggestRule() {
        return (_, builder) -> SharedSuggestionProvider.suggest(CustomRuleValueManager.NAME_TO_RULES.values().stream().map(CarpetRule::name), builder);
    }

    private SuggestionProvider<CommandSourceStack> suggestsNode() {
        return (_, builder) -> SharedSuggestionProvider.suggest(
                PermissionManager.listNode().stream().map(StringArgumentType::escapeIfRequired),
                builder
        );
    }

    private int openDialog(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        DialogProvider provider = ServerComponentCoordinator.getCoordinator(server).getDialogProvider();
        Dialog dialog = provider.getDialog(DialogProvider.START);
        PlayerUtils.openDialog(player, dialog);
        return 1;
    }

    // 设置子命令权限
    private int setLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandPermission permission = PermissionManager.getPermission(StringArgumentType.getString(context, "node"));
        LocalizationKey key = KEY.then("permission");
        if (permission == null) {
            throw CommandUtils.createException(key.then("invalid_node").translate());
        }
        PermissionLevel level;
        try {
            level = PermissionLevel.fromString(StringArgumentType.getString(context, "level"));
        } catch (IllegalArgumentException e) {
            throw CommandUtils.createException(key.then("invalid_value").translate(), e);
        }
        permission.setLevel(level);
        MinecraftServer server = context.getSource().getServer();
        // 向服务器所有玩家发送命令树
        CommandHelper.notifyPlayersCommandsChanged(server);
        try {
            PermissionManager.save(server);
        } catch (IOException e) {
            throw CommandUtils.createIOErrorException(e);
        }
        return level.ordinal();
    }

    /**
     * 显示模组版本
     */
    private int version(CommandContext<CommandSourceStack> context) {
        String name = CarpetOrgAdditionConstants.MOD_NAME;
        Component version = TextBuilder.of(CarpetOrgAdditionConstants.VERSION)
                .setHover(CarpetOrgAdditionConstants.BUILD_TIMESTAMP)
                .build();
        MessageUtils.sendMessage(context, KEY.then("version").translate(name, version));
        return 1;
    }

    // 设置一条规则是否对自己生效
    private int setRuleSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getArgumentPlayer(context);
        if (CommandUtils.isSelfOrFakePlayer(player, context)) {
            MinecraftServer server = ServerUtils.getServer(player);
            CustomRuleValueManager customRuleValueManager = ServerComponentCoordinator.getCoordinator(server).getCustomRuleValueManager();
            String ruleString = StringArgumentType.getString(context, "rule");
            Optional<CustomRuleEntry> optional = CustomRuleValueManager.get(ruleString);
            if (optional.isEmpty()) {
                throw CommandUtils.createException(RULESELF.then("failed").translate());
            }
            CustomRuleEntry entry = optional.get();
            CustomRuleControl<?> control = entry.getControl();
            boolean value = BoolArgumentType.getBool(context, "value");
            customRuleValueManager.setEnabled(player, ruleString, value);
            Component ruleName = RuleUtils.simpleTranslationName(entry.getRule());
            Component playerName = (player == CommandUtils.getSourcePlayer(context) ? LocalizationKeys.Misc.SELF.translate() : player.getDisplayName());
            TextBuilder builder;
            if (value) {
                builder = TextBuilder.of(RULESELF.then("enable").translate(ruleName, playerName));
            } else {
                builder = TextBuilder.of(RULESELF.then("disable").translate(ruleName, playerName));
            }
            if (!control.allowCustomSwitch()) {
                builder.setHover(RULESELF.then("invalid").translate());
                builder.setStrikethrough();
            }
            MessageUtils.sendMessage(context.getSource(), builder.build());
            return 1;
        }
        throw CommandUtils.createSelfOrFakePlayerException();
    }

    private int infoRuleSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getArgumentPlayer(context);
        if (CommandUtils.isSelfOrFakePlayer(player, context)) {
            MinecraftServer server = ServerUtils.getServer(player);
            CustomRuleValueManager valueManager = ServerComponentCoordinator.getCoordinator(server).getCustomRuleValueManager();
            String ruleString = StringArgumentType.getString(context, "rule");
            Optional<CustomRuleEntry> optional = CustomRuleValueManager.get(ruleString);
            if (optional.isEmpty()) {
                throw CommandUtils.createException(RULESELF.then("failed").translate());
            }
            CustomRuleEntry entry = optional.get();
            CustomRuleControl<?> control = entry.getControl();
            MessageUtils.sendEmptyMessage(context);
            LocalizationKey key = RULESELF.then("info");
            MessageUtils.sendMessage(context, key.then("player").translate(player.getDisplayName()));
            boolean enabled = valueManager.isEnabled(player, ruleString);
            Component displayName = RuleUtils.simpleTranslationName(entry.getRule());
            MessageUtils.sendMessage(context, key.then("rule").translate(displayName));
            TextBuilder builder = TextBuilder.of(key.then("enable").translate(TextProvider.getBoolean(enabled)));
            if (!control.allowCustomSwitch()) {
                builder.setHover(RULESELF.then("invalid").translate());
                builder.setStrikethrough();
            }
            MessageUtils.sendMessage(context.getSource(), builder.build());
            return 1;
        }
        throw CommandUtils.createSelfOrFakePlayerException();
    }

    @Override
    public String getDefaultName() {
        return "orange";
    }
}
