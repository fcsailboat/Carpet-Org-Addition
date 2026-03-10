package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.exception.OperationTimeoutException;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.ExperienceTransfer;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigInteger;

public class XpTransferCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("xpTransfer");
    public static final LocalizationKey FAIL = KEY.then("fail");

    public XpTransferCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandXpTransfer))
                .then(Commands.argument("from", EntityArgument.player())
                        .then(Commands.argument("to", EntityArgument.player())
                                .then(Commands.literal("all")
                                        .executes(this::transferAll))
                                .then(Commands.literal("half")
                                        .executes(this::transferHalf))
                                .then(Commands.literal("points")
                                        .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                                .executes(this::transferSpecifyPoint)))
                                .then(Commands.literal("level")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(this::transferSpecifyLevel)))
                                .then(Commands.literal("upgrade")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(this::transferUpgrade)))
                                .then(Commands.literal("upgradeto")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(this::transferUpgradeTo))))));
    }

    /**
     * 转移所有经验
     */
    private int transferAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ExperienceTransfer from = new ExperienceTransfer(getFromPlayer(context));
        ExperienceTransfer to = new ExperienceTransfer(getToPlayer(context));
        // 输出经验的玩家必须是假玩家或者是命令执行者自己
        if (from.isSpecifiedOrFakePlayer(CommandUtils.getSourcePlayer(context))) {
            // 获取转移之前玩家的经验
            int fromBeforeLevel = from.getLevel();
            int toBeforeLevel = to.getLevel();
            BigInteger number;
            try {
                number = from.transferAllTo(to);
            } catch (OperationTimeoutException e) {
                throw CommandUtils.createOperationTimeoutException();
            }
            // 获取转移之后玩家的经验
            int fromCurrentLevel = from.getLevel();
            int toCurrentLevel = to.getLevel();
            TextBuilder builder = TextBuilder.of(
                    KEY.then("all").translate(
                            from.player().getDisplayName(),
                            number.toString(),
                            to.player().getDisplayName()
                    )
            );
            Component hover = getHover(to.player(), toCurrentLevel, toBeforeLevel, from.player(), fromBeforeLevel, fromCurrentLevel);
            builder.setHover(hover);
            MessageUtils.sendMessage(source, builder.build());
            writeLog(source, to.player(), from.player(), number);
            return number.intValue();
        } else {
            // 发送需要目标是自己或假玩家消息
            throw CommandUtils.createSelfOrFakePlayerException();
        }
    }

    /**
     * 转移一半经验
     */
    private int transferHalf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ExperienceTransfer from = new ExperienceTransfer(getFromPlayer(context));
        ExperienceTransfer to = new ExperienceTransfer(getToPlayer(context));
        if (from.isSpecifiedOrFakePlayer(CommandUtils.getSourcePlayer(context))) {
            // 获取转移之前玩家的经验
            int outputBeforeLevel = from.getLevel();
            int inputBeforeLevel = to.getLevel();
            BigInteger number;
            try {
                number = from.transferHalfTo(to);
            } catch (OperationTimeoutException e) {
                throw CommandUtils.createOperationTimeoutException();
            }
            // 获取转移之后玩家的经验
            int outputCurrentLevel = from.getLevel();
            int inputCurrentLevel = to.getLevel();
            Component hover = getHover(to.player(), inputCurrentLevel, inputBeforeLevel, from.player(), outputBeforeLevel, outputCurrentLevel);
            TextBuilder builder = TextBuilder.of(
                    KEY.then("half").translate(
                            from.player().getDisplayName(),
                            number.toString(),
                            to.player().getDisplayName()
                    )
            );
            builder.setHover(hover);
            MessageUtils.sendMessage(source, builder.build());
            writeLog(source, to.player(), from.player(), number);
            return number.intValue();
        } else {
            // 发送消息：只允许操作自己或假玩家
            throw CommandUtils.createSelfOrFakePlayerException();
        }
    }

    /**
     * 转移指定数量的经验
     */
    private int transferSpecifyPoint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(context, "number");
        return this.transfer(context, BigInteger.valueOf(count));
    }

    /**
     * 转移从0级升级到指定等级所需的经验
     */
    private int transferSpecifyLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int level = IntegerArgumentType.getInteger(context, "level");
        BigInteger count;
        try {
            count = ExperienceTransfer.calculateTotalExperience(level, 0);
        } catch (ArithmeticException e) {
            throw CommandUtils.createException(FAIL.then("incalculable").translate(), e);
        }
        return this.transfer(context, count);
    }

    /**
     * 转移从当前等级<b>升级</b>指定等级所需的经验
     */
    private int transferUpgrade(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int level = getToPlayer(context).experienceLevel;
        int upgrade = level + IntegerArgumentType.getInteger(context, "level");
        BigInteger count;
        try {
            count = ExperienceTransfer.calculateUpgradeExperience(level, upgrade);
        } catch (ArithmeticException e) {
            throw CommandUtils.createException(FAIL.then("incalculable").translate(), e);
        }
        return this.transfer(context, count);
    }

    /**
     * 转移从当前等级<b>升级到</b>指定等级所需的经验
     */
    private int transferUpgradeTo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int level = getToPlayer(context).experienceLevel;
        int upgrade = IntegerArgumentType.getInteger(context, "level");
        BigInteger count;
        try {
            count = ExperienceTransfer.calculateUpgradeExperience(level, upgrade);
        } catch (ArithmeticException e) {
            throw CommandUtils.createException(FAIL.then("incalculable").translate(), e);
        }
        if (count.compareTo(BigInteger.ZERO) < 0) {
            // 升级所需经验为负数
            throw CommandUtils.createException(FAIL.then("negative").translate());
        }
        return this.transfer(context, count);
    }

    /**
     * 转移指定数量的经验
     */
    private int transfer(CommandContext<CommandSourceStack> context, BigInteger count) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ExperienceTransfer from = new ExperienceTransfer(getFromPlayer(context));
        ExperienceTransfer to = new ExperienceTransfer(getToPlayer(context));
        // 只能操作自己或假玩家
        if (from.isSpecifiedOrFakePlayer(CommandUtils.getSourcePlayer(context))) {
            // 获取转移之前玩家的经验
            int outputBeforeLevel = from.getLevel();
            int inputBeforeLevel = to.getLevel();
            try {
                from.transferTo(to, count);
            } catch (ExperienceTransfer.ExperienceTransferException e) {
                // 要转移经验的数量不能多于玩家的总经验
                throw CommandUtils.createException(
                        FAIL.then("insufficient").translate(
                                from.player().getDisplayName(),
                                e.getRequire().toString(),
                                e.getExisting().toString()
                        )
                );
            } catch (OperationTimeoutException e) {
                throw CommandUtils.createOperationTimeoutException();
            }
            // 获取转移之后玩家的经验
            int outputCurrentLevel = from.getLevel();
            int inputCurrentLevel = to.getLevel();
            TextBuilder builder = TextBuilder.of(
                    KEY.then("point").translate(
                            from.player().getDisplayName(),
                            count.toString(),
                            to.player().getDisplayName()
                    )
            );
            Component hover = getHover(to.player(), inputCurrentLevel, inputBeforeLevel, from.player(), outputBeforeLevel, outputCurrentLevel);
            builder.setHover(hover);
            MessageUtils.sendMessage(source, builder.build());
            writeLog(source, to.player(), from.player(), count);
            return count.intValue();
        } else {
            // 发送消息：只允许操作自己或假玩家
            throw CommandUtils.createSelfOrFakePlayerException();
        }
    }

    // 获取要输出经验的玩家
    private ServerPlayer getFromPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return EntityArgument.getPlayer(context, "from");
    }

    // 获取要输入经验的玩家
    private ServerPlayer getToPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return EntityArgument.getPlayer(context, "to");
    }

    // 记录日志
    private void writeLog(
            CommandSourceStack source,
            ServerPlayer inputPlayer,
            ServerPlayer outputPlayer,
            BigInteger point
    ) {
        ServerPlayer player = source.getPlayer();
        String output = player == outputPlayer ? "themself" : PlayerUtils.getName(outputPlayer);
        String input = player == inputPlayer ? "themself" : PlayerUtils.getName(inputPlayer);
        CarpetOrgAddition.LOGGER.info(
                "{} transferred {} experience points from {} to {}",
                source.getTextName(),
                point.toString(),
                output,
                input
        );
    }

    // 获取悬停提示
    private Component getHover(
            ServerPlayer inputPlayer,
            int inputCurrentLevel,
            int inputBeforeLevel,
            ServerPlayer outputPlayer,
            int outputBeforeLevel,
            int outputCurrentLevel
    ) {
        return TextBuilder.combineAll(
                TextBuilder.of(
                        KEY.then("upgrade").translate(
                                inputPlayer.getDisplayName(),
                                inputCurrentLevel - inputBeforeLevel,
                                inputBeforeLevel,
                                inputCurrentLevel
                        )
                ).setColor(ChatFormatting.GREEN).build(),
                TextProvider.NEW_LINE,
                TextBuilder.of(
                        KEY.then("degrade").translate(
                                outputPlayer.getDisplayName(),
                                outputBeforeLevel - outputCurrentLevel,
                                outputBeforeLevel,
                                outputCurrentLevel
                        )
                ).setColor(ChatFormatting.RED).build()
        );
    }

    @Override
    public String getDefaultName() {
        return "xpTransfer";
    }
}
