package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.exception.OperationTimeoutException;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.common.CommonTexts;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class XpTransferCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("xpTransfer");
    public static final LocalizationKey FAIL = KEY.then("fail");

    public XpTransferCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(source -> CarpetOrgAdditionSettings.COMMAND_XP_TRANSFER.value().hasPermission(source))
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
            } catch (ExperienceTransferException e) {
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
                CommonTexts.NEW_LINE,
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

    public record ExperienceTransfer(ServerPlayer player) {
        /**
         * {@code int}的最大值
         */
        private static final BigInteger MAX_INTEGER_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
        /**
         * 通过添加经验值能合法提升的最高经验等级
         */
        public static final int MAX_EFFECTIVE_LEVEL = 238609312;
        /**
         * 经验转移的最大时间
         */
        private static final long TIMEOUT_MILLIS = 5000L;
        private static final ThreadPoolExecutor EXPERIENCE_CALCULATE = new ThreadPoolExecutor(
                10,
                10,
                1,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(),
                runnable -> Thread.ofPlatform().daemon().name("Experience Calculate").unstarted(runnable)
        );

        static {
            EXPERIENCE_CALCULATE.allowCoreThreadTimeOut(true);
        }

        /**
         * @return 获取玩家的经验值数
         */
        private int getPoint() {
            int result = Mth.floor(this.player.experienceProgress * (float) this.player.getXpNeededForNextLevel());
            // 避免数值溢出
            return Math.max(result, 0);
        }

        /**
         * 清除玩家的所有经验
         */
        private void clearExperience() {
            this.player.setExperienceLevels(0);
            this.player.setExperiencePoints(0);
        }

        /**
         * 将一名玩家的经验全部转移给另一名玩家
         *
         * @return 转移经验的数量
         */
        public BigInteger transferAllTo(ExperienceTransfer transfer) {
            // 转移大量经验
            return this.transfer(() -> {
                final BigInteger result = this.extractAllExperience();
                this.addExperience(transfer, result);
                return result;
            });
        }

        /**
         * 将一名玩家一半的经验转移给另一名玩家
         *
         * @return 转移经验的数量
         */
        public BigInteger transferHalfTo(ExperienceTransfer transfer) {
            return this.transfer(() -> {
                BigInteger experience = this.extractAllExperience();
                BigInteger half = experience.divide(BigInteger.TWO);
                long time = System.currentTimeMillis();
                this.addExperience(transfer, half, TIMEOUT_MILLIS);
                long timeElapsed = System.currentTimeMillis() - time;
                this.addExperience(this, experience.subtract(half), TIMEOUT_MILLIS - timeElapsed);
                return half;
            });
        }

        /**
         * 转移指定数量的经验
         */
        public void transferTo(ExperienceTransfer transfer, BigInteger count) {
            this.transfer(() -> {
                BigInteger total = this.calculateTotalExperience();
                if (count.compareTo(total) > 0) {
                    throw new XpTransferCommand.ExperienceTransferException(count, total);
                }
                this.clearExperience();
                transfer.addExperience(count);
                this.addExperience(total.subtract(count));
                return count;
            });
        }

        private BigInteger transfer(Supplier<BigInteger> supplier) {
            int level = this.player.experienceLevel;
            int point = this.getPoint();
            try {
                return supplier.get();
            } catch (OperationTimeoutException e) {
                this.player.setExperienceLevels(level);
                this.player.setExperiencePoints(point);
                throw e;
            }
        }

        /**
         * @return 当前玩家是否是假玩家或指定玩家
         */
        public boolean isSpecifiedOrFakePlayer(ServerPlayer specified) {
            return CommandUtils.isSpecifiedOrFakePlayer(this.player, specified);
        }

        /**
         * 添加经验
         *
         * @param experience 添加经验的数量
         */
        private void addExperience(int experience) {
            this.player.giveExperiencePoints(experience);
        }

        private void addExperience(BigInteger bigInteger) {
            this.addExperience(this, bigInteger);
        }

        private void addExperience(ExperienceTransfer transfer, BigInteger experience) {
            this.addExperience(transfer, experience, TIMEOUT_MILLIS);
        }

        private void addExperience(ExperienceTransfer transfer, BigInteger experience, long timeout) {
            int level = transfer.player.experienceLevel;
            int point = transfer.getPoint();
            try {
                long time = System.currentTimeMillis();
                // 经验值过大时舍弃部分经验，避免转移的经验超出预期
                // 这通常是不必要的，因为玩家不太可能在正常的生存模式下得到这么多经验
                if (experience.compareTo(BigInteger.valueOf(1000_0000_0000L)) > 0) {
                    experience = new BigDecimal(experience).multiply(new BigDecimal("0.99999999")).toBigInteger();
                } else if (experience.compareTo(BigInteger.valueOf(100_0000_0000L)) > 0) {
                    experience = new BigDecimal(experience).multiply(new BigDecimal("0.9999999")).toBigInteger();
                }
                while (experience.compareTo(MAX_INTEGER_VALUE) >= 0) {
                    // 经验转移必须在指定时间内完成
                    if (System.currentTimeMillis() - time >= timeout) {
                        throw new OperationTimeoutException();
                    }
                    experience = experience.subtract(MAX_INTEGER_VALUE);
                    transfer.addExperience(Integer.MAX_VALUE);
                }
                transfer.addExperience(experience.intValue());
            } catch (OperationTimeoutException e) {
                // 操作超时，回退经验
                transfer.player.setExperienceLevels(level);
                transfer.player.setExperiencePoints(point);
                throw e;
            }
        }

        /**
         * 计算并清空玩家的经验值
         *
         * @return 玩家的总经验数
         */
        private BigInteger extractAllExperience() {
            BigInteger experience = calculateTotalExperience();
            // 清除玩家的所有经验
            this.clearExperience();
            return experience;
        }

        /**
         * 计算玩家的总经验值
         *
         * @apiNote 转移大量经验时，实际转移的经验数量可能高于预期，因此方法返回结果前可能会对超出部分的经验进行舍弃
         * @see <a href="https://report.bugs.mojang.com/servicedesk/customer/portal/2/MC-271084">MC-271084</a>
         */
        private BigInteger calculateTotalExperience() {
            int level = Math.min(this.player.experienceLevel, MAX_EFFECTIVE_LEVEL);
            int xp = this.player.experienceLevel >= MAX_EFFECTIVE_LEVEL ? 0 : this.getPoint();
            if (level <= 31) {
                // 作者：ChatGPT
                int totalExp;
                // 0-16级
                if (level <= 16) {
                    totalExp = level * level + 6 * level;
                }
                // 17-31级
                else {
                    totalExp = (int) (2.5 * level * level - 40.5 * level + 360);
                }
                return BigInteger.valueOf(totalExp);
            }
            BigDecimal bigLevel = BigDecimal.valueOf(level);
            BigDecimal decimal = new BigDecimal("4.5").multiply(bigLevel.multiply(bigLevel))
                    .subtract(new BigDecimal("162.5").multiply(bigLevel))
                    .add(new BigDecimal("2220"))
                    .add(new BigDecimal(xp));
            return decimal.toBigInteger();
        }

        public int getLevel() {
            return this.player.experienceLevel;
        }

        /**
         * 根据经验等级和经验值计算总经验值
         *
         * @param level 经验等级
         * @param xp    经验值
         * @return 总经验值
         */
        public static BigInteger calculateTotalExperience(int level, int xp) {
            BigInteger experience = calculateUpgradeExperience(0, level);
            return xp == 0 ? experience : experience.add(BigInteger.valueOf(xp));
        }

        /**
         * 计算从指定等级升级到指定等级所需的经验数量
         *
         * @param level  当前经验等级
         * @param target 目标经验等级
         * @return 从当前等级升级到目标等级需要的经验数量
         * @throws ArithmeticException 如果目标等级超过了能通过添加经验值能合法提升的最高经验等级
         * @see <a href="https://zh.minecraft.wiki/w/%E7%BB%8F%E9%AA%8C#%E7%BB%8F%E9%AA%8C%E7%AD%89%E7%BA%A7">经验等级</a>
         */
        public static BigInteger calculateUpgradeExperience(int level, int target) {
            if (target > MAX_EFFECTIVE_LEVEL) {
                // 译：升级到%s级需要无限的经验
                throw new ArithmeticException("Upgrading to level %s requires infinite experience".formatted(target));
            }
            if (level == target) {
                return BigInteger.ZERO;
            }
            if (level > target) {
                return calculateUpgradeExperience(target, level).negate();
            }
            double sum = 0;
            // 等差数列的首项
            int start;
            // 等差数列的末项
            int end;
            // 只计算到目标等级的上一级
            target--;
            // 计算第一个区间：0 <= 等级 <= 15
            start = level;
            end = Math.min(target, 15);
            if (start <= end) {
                sum += calculateExperienceSum(start, end, x -> 2.0 * x + 7);
            }
            // 计算第二个区间：16 <= 等级 <= 30
            start = Math.max(level, 16);
            end = Math.min(target, 30);
            if (start <= end) {
                sum += calculateExperienceSum(start, end, x -> 5.0 * x - 38);
            }
            // 计算第三个区间：等级 >= 31
            start = Math.max(level, 31);
            end = target;
            if (start <= end) {
                sum += calculateExperienceSum(start, end, x -> 9.0 * x - 158);
            }
            return BigDecimal.valueOf(sum).toBigInteger();
        }

        public static CompletableFuture<Int2FloatMap.Entry> calculateUpgradeLevel(BigInteger totalPoints, BooleanSupplier interrupt) {
            if (totalPoints.signum() < 0) {
                throw new IllegalArgumentException("totalPoints must not be negative, was: " + totalPoints);
            }
            if (totalPoints.signum() == 0) {
                return CompletableFuture.completedFuture(Int2FloatMap.entry(0, 0F));
            }
            return CompletableFuture.supplyAsync(() -> {
                XpTransferCommand.MockPlayer mock = new XpTransferCommand.MockPlayer();
                BigInteger total = totalPoints;
                while (total.compareTo(MAX_INTEGER_VALUE) >= 0) {
                    if (Thread.currentThread().isInterrupted() || interrupt.getAsBoolean()) {
                        throw new CancellationException("Experience calculation interrupted");
                    }
                    total = total.subtract(MAX_INTEGER_VALUE);
                    mock.addExperience(Integer.MAX_VALUE);
                }
                mock.addExperience(total.intValueExact());
                return Int2FloatMap.entry(mock.getExperienceLevel(), mock.getExperienceProgress());
            }, EXPERIENCE_CALCULATE);
        }

        /**
         * 计算每个区间的经验总和
         *
         * @param function 每个区间的经验计算公式
         */
        private static double calculateExperienceSum(int start, int end, IntFunction<Double> function) {
            // 计算等差数列的项数（同时也是从当前等级到目标等级需要升的等级数）
            int n = end - start + 1;
            return MathUtils.sumOfArithmeticProgression(function.apply(start), function.apply(end), n);
        }
    }

    public static class MockPlayer {
        private int experienceLevel;
        private int totalExperience;
        private float experienceProgress;

        public MockPlayer() {
        }

        public MockPlayer(int level) {
            this.addExperienceLevels(level);
        }

        public void addExperienceLevels(int levels) {
            this.experienceLevel += levels;
            if (this.experienceLevel < 0) {
                this.experienceLevel = 0;
                this.experienceProgress = 0.0F;
                this.totalExperience = 0;
            }
        }

        public int getNextLevelExperience() {
            if (this.experienceLevel >= 30) {
                return 112 + (this.experienceLevel - 30) * 9;
            } else {
                return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
            }
        }

        public void addExperience(int experience) {
            this.experienceProgress = this.experienceProgress + (float) experience / (float) this.getNextLevelExperience();
            this.totalExperience = Mth.clamp(this.totalExperience + experience, 0, Integer.MAX_VALUE);
            while (this.experienceProgress < 0.0F) {
                float f = this.experienceProgress * (float) this.getNextLevelExperience();
                if (this.experienceLevel > 0) {
                    this.addExperienceLevels(-1);
                    this.experienceProgress = 1.0F + f / (float) this.getNextLevelExperience();
                } else {
                    this.addExperienceLevels(-1);
                    this.experienceProgress = 0.0F;
                }
            }
            while (this.experienceProgress >= 1.0F) {
                this.experienceProgress = (this.experienceProgress - 1.0F) * (float) this.getNextLevelExperience();
                this.addExperienceLevels(1);
                this.experienceProgress = this.experienceProgress / (float) this.getNextLevelExperience();
            }
        }

        public int getPoint() {
            return Mth.floor(this.experienceProgress * (float) this.getNextLevelExperience());
        }

        public void clearExperience() {
            this.experienceLevel = 0;
            this.experienceProgress = 0;
        }

        public int getExperienceLevel() {
            return experienceLevel;
        }

        public float getExperienceProgress() {
            return this.experienceProgress;
        }
    }

    public static class ExperienceTransferException extends RuntimeException {
        /**
         * 需要的经验
         */
        private final Number require;
        /**
         * 现有的经验
         */
        private final Number existing;

        public ExperienceTransferException(Number require, Number existing) {
            this.require = require;
            this.existing = existing;
        }

        public Number getRequire() {
            return require;
        }

        public Number getExisting() {
            return existing;
        }
    }
}
