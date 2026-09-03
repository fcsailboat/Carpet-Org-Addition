package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.network.event.CustomClickEvents;
import boat.carpetorgaddition.network.event.CustomClickKeys;
import boat.carpetorgaddition.rule.value.OpenPlayerInventoryCommandOption;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.ItemStackStatistics;
import boat.carpetorgaddition.wheel.ProgressBar;
import boat.carpetorgaddition.wheel.common.CommonCommands;
import boat.carpetorgaddition.wheel.common.CommonTexts;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import boat.carpetorgaddition.wheel.inventory.SimulatePlayerInventory;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.CarpetSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class OfflinePlayerInventorySearchTask extends AbstractOfflinePlayerSearchTask {
    /**
     * 已找到的物品数量
     */
    private final AtomicInteger itemCount = new AtomicInteger();
    /**
     * 在已找到的物品中，是否包含在嵌套的容器中找到的物品
     */
    private final AtomicBoolean shulkerBox = new AtomicBoolean(false);
    private final ItemStackPredicate predicate;
    private final List<Result> results = Collections.synchronizedList(new ArrayList<>());
    public static final LocalizationKey KEY = ItemSearchTask.KEY.then("offline_player");

    public OfflinePlayerInventorySearchTask(CommandSourceStack source, ItemStackPredicate predicate, ServerPlayer player) {
        super(source, player);
        this.predicate = predicate;
    }

    @Override
    protected void sendProgress(LocalizationKey key, @NonNull ProgressBar progressBar) {
        MessageUtils.sendMessageToHud(this.player, key.translate(this.predicate.getDisplayName(), progressBar.getDisplay()));
    }

    @Override
    protected void search(@NonNull CompoundTag nbt, NameAndId entry, boolean unknownPlayer) {
        // 统计物品栏物品
        statistics(this.getInventory(nbt), entry, unknownPlayer, PlayerInventoryType.INVENTORY);
        statistics(this.getEnderChest(nbt), entry, unknownPlayer, PlayerInventoryType.ENDER_CHEST);
    }

    /**
     * 统计物品
     */
    private void statistics(Container inventory, NameAndId entry, boolean unknownPlayer, PlayerInventoryType type) {
        ItemStackStatistics statistics = new ItemStackStatistics(this.predicate);
        statistics.statistics(inventory);
        if (statistics.getSum() == 0) {
            return;
        }
        this.itemCount.addAndGet(statistics.getSum());
        if (statistics.hasNestingItem()) {
            this.shulkerBox.set(true);
        }
        Result result = new Result(entry, statistics, unknownPlayer, type, this.server);
        this.results.add(result);
    }

    // 获取玩家物品栏
    private Container getInventory(CompoundTag nbt) {
        return SimulatePlayerInventory.of(nbt, this.server);
    }

    /**
     * 从NBT读取玩家末影箱
     *
     * @see Player#readAdditionalSaveData(ValueInput)
     */
    @SuppressWarnings("JavadocReference")
    protected Container getEnderChest(CompoundTag nbt) {
        PlayerEnderChestContainer inventory = new PlayerEnderChestContainer();
        ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, ServerUtils.getWorld(this.player).registryAccess(), nbt);
        inventory.fromSlots(readView.listOrEmpty("EnderItems", ItemStackWithSlot.CODEC));
        return inventory;
    }

    // 发送命令反馈
    @Override
    protected boolean sendFeedback() {
        if (this.results.isEmpty()) {
            MessageUtils.sendMessage(this.source, KEY.then("cannot_find").translate(this.predicate.getDisplayName()));
            return true;
        }
        int resultCount = this.results.size();
        this.results.sort((o1, o2) -> o2.statistics().getSum() - o1.statistics().getSum());
        this.pagedCollection.addContent(this.results);
        Component itemCount = getItemCount();
        Component numberOfPeople = getNumberOfPeople(resultCount);
        Component message = getFirstFeedback(numberOfPeople, itemCount);
        TextBuilder builder = TextBuilder.of(message);
        builder.setHover(KEY.then("prompt").translate());
        MessageUtils.sendEmptyMessage(this.source);
        MessageUtils.sendMessage(this.source, builder.build());
        CommandUtils.handlingException(this.pagedCollection::print, source);
        return true;
    }

    /**
     * 获取首条反馈消息
     */
    private Component getFirstFeedback(Component numberOfPeople, Component itemCount) {
        return KEY.then("head").translate(numberOfPeople, itemCount, this.predicate.getDisplayName());
    }

    /**
     * 获取物品数量文本
     */
    private Component getItemCount() {
        Optional<Item> optional = this.predicate.getConvert();
        if (optional.isPresent()) {
            return FinderCommand.showCount(optional.get().getDefaultInstance(), this.itemCount.get(), this.shulkerBox.get());
        } else {
            TextBuilder builder = TextBuilder.of(this.itemCount);
            return this.shulkerBox.get() ? builder.setItalic().build() : builder.build();
        }
    }

    /**
     * 获取玩家数量文本
     */
    private Component getNumberOfPeople(int resultCount) {
        // 玩家总数的悬停提示
        ArrayList<Component> peopleHover = new ArrayList<>();
        peopleHover.add(KEY.then("total").translate(this.total));
        peopleHover.add(KEY.then("found").translate(resultCount));
        TextBuilder builder = TextBuilder.of(resultCount);
        builder.setHover(TextBuilder.joinList(peopleHover));
        // 玩家总数文本
        return builder.build();
    }

    private Component getContainerName(PlayerInventoryType type) {
        TextBuilder builder = TextBuilder.of(type.getDisplayName());
        switch (type) {
            case INVENTORY -> builder.setColor(ChatFormatting.YELLOW);
            case ENDER_CHEST -> builder.setColor(ChatFormatting.DARK_PURPLE);
        }
        return builder.build();
    }

    /**
     * 添加打开玩家物品栏按钮
     */
    @Nullable
    protected Component openInventoryButton(NameAndId entry) {
        if (this.canOpenOfflinePlayerInventory(source)) {
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            writer.putUuid(CustomClickKeys.UUID, entry.id());
            writer.putPlayerInventoryType(CustomClickKeys.INVENTORY_TYPE, PlayerInventoryType.INVENTORY);
            TextBuilder builder = TextBuilder.of("[O]");
            builder.setCustomEvent(CustomClickEvents.OPEN_INVENTORY, writer);
            builder.setHover(LocalizationKeys.Operation.OpenInventory.HOVER.translate(LocalizationKeys.Misc.INVENTORY.translate()));
            builder.setColor(ChatFormatting.GRAY);
            return builder.build();
        }
        return null;
    }

    @Nullable
    private Component openEnderChestButton(NameAndId entry) {
        if (this.canOpenOfflinePlayerInventory(source)) {
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            writer.putUuid(CustomClickKeys.UUID, entry.id());
            writer.putPlayerInventoryType(CustomClickKeys.INVENTORY_TYPE, PlayerInventoryType.ENDER_CHEST);
            TextBuilder builder = TextBuilder.of("[O]");
            builder.setCustomEvent(CustomClickEvents.OPEN_INVENTORY, writer);
            builder.setHover(LocalizationKeys.Operation.OpenInventory.HOVER.translate(LocalizationKeys.Misc.ENDER_CHEST.translate()));
            builder.setColor(ChatFormatting.GRAY);
            return builder.build();
        }
        return null;
    }

    /**
     * @return 玩家是否可以打开离线玩家物品栏
     */
    private boolean canOpenOfflinePlayerInventory(CommandSourceStack source) {
        return CommandUtils.canUseCommand(source, CarpetSettings.commandPlayer)
               && OpenPlayerInventoryCommandOption.isEnable(source)
               && CarpetOrgAdditionSettings.PLAYER_COMMAND_OPEN_PLAYER_INVENTORY_OPTION.value().canOpenOfflinePlayer();
    }

    @Override
    protected LocalizationKey getLocalizationKey() {
        return KEY;
    }

    /**
     * @apiNote 非静态的内部类强引用了外部类导致暂时无法被回收，但这不是问题
     */
    public class Result implements Supplier<Component> {
        private final MinecraftServer server;
        private final NameAndId playerConfigEntry;
        private final ItemStackStatistics statistics;
        private final boolean isUnknown;
        private final PlayerInventoryType type;

        private Result(NameAndId playerConfigEntry, ItemStackStatistics statistics, boolean isUnknown, PlayerInventoryType type, MinecraftServer server) {
            this.playerConfigEntry = playerConfigEntry;
            this.statistics = statistics;
            this.isUnknown = isUnknown;
            this.type = type;
            this.server = server;
        }

        @Override
        public Component get() {
            // 获取玩家名，并添加UUID悬停提示
            String name = playerConfigEntry.name();
            String uuid = playerConfigEntry().id().toString();
            // 悬停提示
            Component hover = TextBuilder.combineAll("UUID: %s\n".formatted(uuid), CommonTexts.COPY_CLICK);
            // 获取物品数量，如果包含在潜影盒中找到的物品，就设置物品为斜体
            Component count = statistics().getCountText();
            TextBuilder builder = getDisplayPlayerName(name, uuid, hover, count, this.type);
            return builder.build();
        }

        // 获取玩家显示名称
        private TextBuilder getDisplayPlayerName(String name, String uuid, Component hover, Component count, PlayerInventoryType type) {
            boolean unknown = isUnknown();
            TextJoiner joiner = new TextJoiner();
            if (unknown) {
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
            Component button = switch (type) {
                case INVENTORY -> openInventoryButton(this.playerConfigEntry());
                case ENDER_CHEST -> openEnderChestButton(this.playerConfigEntry());
            };
            joiner.append(button);
            TextBuilder builder = TextBuilder.of(joiner.join())
                    .setHover(hover)
                    .setColor(ChatFormatting.GRAY);
            Component container = getContainerName(type);
            return KEY.then("each").builder(builder.build(), container, count);
        }

        // 创建单击上线按钮
        private Component createLoginButton() {
            if (CommandUtils.canUseCommand(source, CarpetSettings.commandPlayer)) {
                String command = CommonCommands.spawnFakePlayer(playerConfigEntry().name());
                TextBuilder builder = TextBuilder.of(" [↑]");
                builder.setCommand(command);
                builder.setHover(LocalizationKeys.Button.LOGIN.translate());
                return builder.build();
            }
            return TextBuilder.empty();
        }

        public NameAndId playerConfigEntry() {
            return playerConfigEntry;
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
            writer.putUuid(CustomClickKeys.UUID, playerConfigEntry().id());
            button.setCustomEvent(CustomClickEvents.QUERY_PLAYER_NAME, writer);
            // 设置按钮悬停提示
            button.setHover(TextBuilder.joinList(list));
            return button.build();
        }

        public ItemStackStatistics statistics() {
            return statistics;
        }

        public boolean isUnknown() {
            return isUnknown;
        }
    }
}
