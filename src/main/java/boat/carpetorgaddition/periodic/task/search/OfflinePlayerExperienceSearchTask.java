package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.network.event.CustomClickEvents;
import boat.carpetorgaddition.network.event.CustomClickKeys;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.ProgressBar;
import boat.carpetorgaddition.wheel.misc.ExperienceTransfer;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class OfflinePlayerExperienceSearchTask extends AbstractOfflinePlayerSearchTask {
    public static final LocalizationKey KEY = FinderCommand.KEY.then("xp").then("offline_player");
    private final AtomicReference<BigInteger> totals = new AtomicReference<>(BigInteger.ZERO);
    private final List<Result> results = Collections.synchronizedList(new ArrayList<>());
    private CompletableFuture<Int2FloatMap.Entry> calculate;

    public OfflinePlayerExperienceSearchTask(CommandSourceStack source, ServerPlayer player) {
        super(source, player);
    }

    @Override
    protected void sendProgress(LocalizationKey key, @NonNull ProgressBar progressBar) {
        MessageUtils.sendMessageToHud(this.player, key.translate(progressBar.getDisplay()));
    }

    @Override
    protected boolean sendFeedback() {
        LocalizationKey key = this.getLocalizationKey();
        if (this.results.isEmpty()) {
            MessageUtils.sendMessage(this.source, key.then("cannot_find").translate());
            return true;
        }
        if (this.calculate == null) {
            this.calculate = ExperienceTransfer.calculateUpgradeLevel(this.totals.get(), this::isCancelled);
        }
        return switch (this.calculate.state()) {
            case RUNNING -> false;
            case CANCELLED -> true;
            case SUCCESS -> {
                Int2FloatMap.Entry entry = this.calculate.join();
                this.results.sort(Comparator.comparing(o -> o.experienceValue));
                this.pagedCollection.addContent(this.results);
                MessageUtils.sendEmptyMessage(this.source);
                String format = MathUtils.formatToMaxTwoDecimals(entry.getIntKey() + entry.getFloatValue());
                Component sumLevel = TextBuilder.of(format)
                        .setHover(this.totals.get())
                        .setColor(ChatFormatting.GRAY)
                        .build();
                Component head = key.then("head")
                        .builder(this.results.size(), sumLevel)
                        .setHover(key.then("prompt").translate())
                        .build();
                MessageUtils.sendMessage(this.source, head);
                CommandUtils.handlingException(this.pagedCollection::print, source);
                yield true;
            }
            case FAILED -> throw new TaskExecutionException(
                    () -> MessageUtils.sendErrorMessage(
                            player,
                            key.then("fail").then("incalculable").translate(),
                            calculate.exceptionNow()));
        };
    }

    @Override
    protected void search(CompoundTag nbt, NameAndId entry, boolean unknownPlayer) {
        ExperienceValue value;
        try {
            value = this.readExperienceValue(nbt);
        } catch (ArithmeticException e) {
            return;
        }
        BigInteger total = ExperienceTransfer.calculateTotalExperience(value.level(), value.point());
        if (value.level() == 0 && value.point() == 0) {
            return;
        }
        this.includeTotal(total);
        this.results.add(new Result(this.server, entry, value, unknownPlayer, this.source));
    }

    private ExperienceValue readExperienceValue(CompoundTag nbt) {
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, this.server.registryAccess(), nbt);
        int level = input.getIntOr("XpLevel", 0);
        float progress = input.getFloatOr("XpP", 0F);
        int point = getXpPoint(level, progress);
        return new ExperienceValue(level, progress, point);
    }

    private int getXpPoint(int level, float progress) {
        return Mth.floor(progress * (float) this.getXpNeededForNextLevel(level));
    }

    private int getXpNeededForNextLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else {
            return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
        }
    }

    private void includeTotal(BigInteger value) {
        this.totals.accumulateAndGet(value, BigInteger::add);
    }

    @Override
    protected LocalizationKey getLocalizationKey() {
        return KEY;
    }

    public record ExperienceValue(int level, float progress, int point, BigInteger total) implements Comparable<ExperienceValue> {
        public ExperienceValue(int level, float progress, int point) {
            int correct = Math.min(level, ExperienceTransfer.MAX_EFFECTIVE_LEVEL);
            BigInteger total = ExperienceTransfer.calculateTotalExperience(correct, point);
            this(correct, progress, point, total);
        }

        @Override
        public int compareTo(@NonNull ExperienceValue o) {
            int compare = Integer.compare(this.level(), o.level());
            return -(compare == 0 ? Integer.compare(this.point(), o.point()) : compare);
        }
    }

    public static class Result implements Supplier<Component> {
        private final MinecraftServer server;
        private final NameAndId nameAndId;
        private final ExperienceValue experienceValue;
        private final boolean unknownPlayer;
        private final CommandSourceStack source;

        private Result(MinecraftServer server, NameAndId nameAndId, ExperienceValue experienceValue, boolean unknownPlayer, CommandSourceStack source) {
            this.server = server;
            this.nameAndId = nameAndId;
            this.experienceValue = experienceValue;
            this.unknownPlayer = unknownPlayer;
            this.source = source;
        }

        @Override
        public Component get() {
            // 获取玩家名，并添加UUID悬停提示
            String name = this.nameAndId.name();
            String uuid = this.nameAndId.id().toString();
            // 悬停提示
            Component hover = TextBuilder.combineAll("UUID: %s\n".formatted(uuid), TextProvider.COPY_CLICK);
            String format = MathUtils.formatToMaxTwoDecimals(this.experienceValue.level() + this.experienceValue.progress());
            Component level = TextBuilder.of(format)
                    .setColor(ChatFormatting.GRAY)
                    .setHover(this.experienceValue.total())
                    .build();
            TextBuilder builder = getDisplayPlayerName(name, uuid, hover, level);
            return builder.build();
        }

        // 获取玩家显示名称
        private TextBuilder getDisplayPlayerName(String name, String uuid, Component hover, Component count) {
            TextJoiner joiner = new TextJoiner();
            if (this.unknownPlayer) {
                Component displayName = TextBuilder.of(name)
                        .setStrikethrough()
                        .setCopyToClipboard(uuid, false)
                        .build();
                joiner.append(displayName).append(createSearchButton());
            } else {
                Component displayName = TextBuilder.of("[" + name + "]")
                        .setCopyToClipboard(name, false)
                        .build();
                joiner.append(displayName).append(createLoginButton());
            }
            TextBuilder builder = TextBuilder.of(joiner.join())
                    .setHover(hover)
                    .setColor(ChatFormatting.GRAY);
            return KEY.then("each").builder(builder.build(), count);
        }

        // 创建单击上线按钮
        private Component createLoginButton() {
            if (CommandUtils.canUseCommand(this.source, CarpetSettings.commandPlayer)) {
                String command = CommandProvider.spawnFakePlayer(this.nameAndId.name());
                TextBuilder builder = TextBuilder.of(" [↑]");
                builder.setCommand(command);
                builder.setHover(LocalizationKeys.Button.LOGIN.translate());
                return builder.build();
            }
            return TextBuilder.empty();
        }

        // 创建查询玩家名称按钮
        private Component createSearchButton() {
            // 按钮的悬停提示
            ArrayList<Component> list = new ArrayList<>();
            list.add(LocalizationKeys.Operation.QueryPlayerName.Hover.FIRST.translate());
            list.add(TextBuilder.of(LocalizationKeys.Operation.QueryPlayerName.Hover.SECOND.translate()).setColor(ChatFormatting.RED).build());
            TextBuilder button = TextBuilder.of(" [\uD83D\uDD0D]");
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            // 设置单击查询玩家名称
            writer.putUuid(CustomClickKeys.UUID, this.nameAndId.id());
            button.setCustomEvent(CustomClickEvents.QUERY_PLAYER_NAME, writer);
            // 设置按钮悬停提示
            button.setHover(TextBuilder.joinList(list));
            return button.build();
        }
    }
}
