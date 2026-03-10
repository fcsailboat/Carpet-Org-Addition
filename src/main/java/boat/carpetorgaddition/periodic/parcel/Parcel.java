package boat.carpetorgaddition.periodic.parcel;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.CommandRegister;
import boat.carpetorgaddition.command.MailCommand;
import boat.carpetorgaddition.dataupdate.nbt.ParcelDataUpdater;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.SimpleCounter;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.inventory.ImmutableInventory;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static boat.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask.UNKNOWN;

/**
 * 快递
 */
public class Parcel implements Comparable<Parcel> {
    /**
     * 寄件人
     */
    private final String sender;
    /**
     * 收件人
     */
    private final String recipient;
    /**
     * 快递中包含的物品
     */
    private final Container parcels;
    /**
     * 快递是否已被撤回
     */
    private boolean recall = false;
    /**
     * 快递单号
     */
    private final int id;
    /**
     * 物品接收者的UUID
     */
    @Nullable
    private final UUID uuid;
    private final MinecraftServer server;
    private final LocalDateTime time;
    private final WorldFormat worldFormat;
    public static final String EXPRESS = "express";
    private static final int CURRENT_VERSION = 3;

    public Parcel(MinecraftServer server, ServerPlayer sender, ServerPlayer recipient, int id) throws CommandSyntaxException {
        this(server, sender, recipient.getGameProfile(), getPlayerHandStack(sender), id);
    }

    public Parcel(MinecraftServer server, ServerPlayer sender, GameProfile gameProfile, ItemStack itemStack, int id) {
        this(server, PlayerUtils.getName(sender), gameProfile.name(), gameProfile.id(), itemStack, id, LocalDateTime.now());
    }

    public Parcel(MinecraftServer server, ServerPlayer sender, GameProfile gameProfile, int id) throws CommandSyntaxException {
        this(server, sender, gameProfile, getPlayerHandStack(sender), id);
    }

    private Parcel(MinecraftServer server, String sender, String recipient, @Nullable UUID uuid, ItemStack parcel, int id, LocalDateTime time) {
        this.server = server;
        this.sender = sender;
        this.recipient = recipient;
        this.uuid = uuid;
        this.parcels = new ImmutableInventory(parcel);
        this.id = id;
        this.time = time;
        this.worldFormat = new WorldFormat(server, EXPRESS);
    }

    private Parcel(MinecraftServer server, String sender, String recipient, @Nullable UUID uuid, Container container, int id, LocalDateTime time) {
        this.server = server;
        this.sender = sender;
        this.recipient = recipient;
        this.uuid = uuid;
        this.parcels = container;
        this.id = id;
        this.time = time;
        this.worldFormat = new WorldFormat(server, EXPRESS);
    }

    public Parcel(MinecraftServer server, ServerPlayer sourcePlayer, GameProfile recipient, AutoGrowInventory autoGrowInventory, int id) {
        this(server, PlayerUtils.getName(sourcePlayer), recipient.name(), recipient.id(), autoGrowInventory, id, LocalDateTime.now());
    }

    private static ItemStack getPlayerHandStack(ServerPlayer player) throws CommandSyntaxException {
        ItemStack mainHandStack = player.getMainHandItem();
        if (mainHandStack.isEmpty()) {
            ItemStack offHandStack = player.getOffhandItem();
            if (offHandStack.isEmpty()) {
                throw CommandUtils.createException(MailCommand.SEND.then("prompt").translate());
            }
            return offHandStack.copyAndClear();
        } else {
            return mainHandStack.copyAndClear();
        }
    }

    /**
     * 发送快递
     */
    public void send() {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer senderPlayer = playerManager.getPlayerByName(this.sender);
        ServerPlayer recipientPlayer = playerManager.getPlayerByName(this.recipient);
        if (senderPlayer == null) {
            CarpetOrgAddition.LOGGER.error("The parcel delivery is sent by non-existent player");
            return;
        }
        // 向快递发送者发送发出快递的消息
        Component recall = TextProvider.clickRun(CommandProvider.recallParcel(this.getId(), false));
        LocalizationKey key = MailCommand.SEND;
        Object name = recipientPlayer == null ? this.recipient : recipientPlayer.getDisplayName();
        MessageUtils.sendMessage(senderPlayer, key.then("sender").translate(name, this.getCount(), this.getDisplayName(), recall));
        // 向快递接受者发送发出快递的消息
        Component receive = TextProvider.clickRun(CommandProvider.collectParcel(this.getId(), false));
        if (recipientPlayer == null) {
            TextBuilder builder = TextBuilder.of(key.then("offline").translate());
            builder.setGrayItalic();
            MessageUtils.sendMessage(senderPlayer, builder.build());
        } else {
            MessageUtils.sendMessage(recipientPlayer, key.then("recipient").translate(senderPlayer.getDisplayName(), this.getCount(), this.getDisplayName(), receive));
            // 在接收者位置播放音效
            playXpOrbPickupSound(recipientPlayer);
        }
        CarpetOrgAddition.LOGGER.info("{} sent {} {} to {}", this.sender, this.getCount(), this.getDisplayName().getString(), this.recipient);
    }

    /**
     * 接收快递
     */
    public void collect() throws IOException {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer player = playerManager.getPlayerByName(this.recipient);
        if (player == null) {
            CarpetOrgAddition.LOGGER.warn("The player who received the package does not exist");
            return;
        }
        LocalizationKey key = MailCommand.COLLECT;
        if (this.recall) {
            // 快递已被撤回
            MessageUtils.sendMessage(player, key.then("recalled").translate());
            return;
        }
        // 接收之前的物品数量
        int count = this.getCount();
        Component name = this.getDisplayName();
        Counter<Item> before = new SimpleCounter<>();
        this.getParcels().forEach(itemStack -> before.add(itemStack.getItem(), itemStack.getCount()));
        InsertResult result = insertStack(player, false);
        Counter<Item> after = new SimpleCounter<>();
        this.getParcels().forEach(itemStack -> after.add(itemStack.getItem(), itemStack.getCount()));
        Counter<Item> counter = new SimpleCounter<>();
        for (Item item : before) {
            int remaining = before.getCount(item) - after.getCount(item);
            if (remaining > 0) {
                counter.add(item, remaining);
            }
        }
        switch (result) {
            case COMPLETE -> {
                // 物品完全接收
                MessageUtils.sendMessage(player, key.then("success").translate(count, name));
                // 通知发送者物品已接收
                MessageUtils.sendMessageIfPlayerOnline(this.server, this.sender, () -> ParcelManager.getReceiveNotice(player, counter));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player, key.then("partial_reception").translate(count - surplusCount, surplusCount));
                // 通知发送者物品已接收
                MessageUtils.sendMessageIfPlayerOnline(this.server, this.sender, () -> ParcelManager.getReceiveNotice(player, counter));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case FAIL -> MessageUtils.sendMessage(player, key.then("insufficient_capacity").translate());
        }
    }

    /**
     * 撤回快递
     */
    public void recall() throws IOException {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer player = playerManager.getPlayerByName(this.sender);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("The player who recall the package does not exist");
            return;
        }
        // 接收之前的物品数量
        int count = this.getCount();
        Component name = this.getDisplayName();
        // 将快递内容放入物品栏
        final LocalizationKey key = MailCommand.RECALL;
        this.recall = true;
        switch (insertStack(player, true)) {
            case COMPLETE -> {
                MessageUtils.sendMessage(player, key.then("success").translate(count, name));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int remaining = this.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player, key.then("partial_reception").translate(count - remaining, remaining));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            // 物品未能成功放入物品栏
            case FAIL -> MessageUtils.sendMessage(player, key.then("insufficient_capacity").translate());
            default -> throw new IllegalStateException();
        }
        // 如果接收者存在，向接收者发送物品被撤回的消息
        MessageUtils.sendMessageIfPlayerOnline(this.server, this.recipient,
                () -> TextBuilder.of(MailCommand.NOTICE.then("recall").translate(player.getDisplayName()))
                        .setGrayItalic()
                        .build()
        );
    }

    /**
     * 拦截快递
     */
    public void intercept(ServerPlayer operator) throws IOException {
        int count = this.getCount();
        Component name = this.getDisplayName();
        LocalizationKey key = MailCommand.INTERCEPT;
        switch (insertStack(operator, false)) {
            case COMPLETE -> {
                MessageUtils.sendMessage(operator, key.then("success").translate(count, name));
                playItemPickupSound(operator);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.getCount();
                MessageUtils.sendMessage(operator, key.then("partial_reception").translate(count - surplusCount, surplusCount));
                playItemPickupSound(operator);
            }
            case FAIL -> {
                MessageUtils.sendMessage(operator, key.then("insufficient_capacity").translate());
                return;
            }
        }
        Component hover = TextBuilder.combineAll(name, "*", count - this.getCount());
        LocalizationKey noticeKey = MailCommand.NOTICE.then("intercept");
        MessageUtils.sendMessageIfPlayerOnline(this.server, this.sender, player -> {
            Component text = getOperatorPlayerName(operator, player);
            TextBuilder builder = TextBuilder.of(noticeKey.then("sender").translate(text, this.recipient));
            builder.setGrayItalic();
            builder.setHover(hover);
            return builder.build();
        });
        MessageUtils.sendMessageIfPlayerOnline(this.server, this.recipient, player -> {
            Component text = getOperatorPlayerName(operator, player);
            TextBuilder builder = TextBuilder.of(noticeKey.then("recipient").translate(text, this.sender));
            builder.setGrayItalic();
            builder.setHover(hover);
            return builder.build();
        });
    }

    private Component getOperatorPlayerName(ServerPlayer operator, ServerPlayer player) {
        MailCommand instance = CommandRegister.getCommandInstance(MailCommand.class);
        return instance.getInterceptPredicate().test(player.createCommandSourceStack()) ? operator.getDisplayName() : LocalizationKeys.Misc.OPERATOR.translate();
    }

    /**
     * 播放物品拾取音效
     */
    public static void playItemPickupSound(@NotNull ServerPlayer player) {
        ServerUtils.playSound(player, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
    }

    /**
     * 播放经验球拾取音效
     */
    public static void playXpOrbPickupSound(@NotNull ServerPlayer player) {
        ServerUtils.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS);
    }

    /**
     * 向物品栏里插入物品
     */
    private InsertResult insertStack(ServerPlayer player, boolean forceSave) throws IOException {
        int count = this.getCount();
        this.getParcels().forEach(player.getInventory()::add);
        // 物品没有插入
        if (count == this.getCount()) {
            if (forceSave) {
                this.save();
            }
            return InsertResult.FAIL;
        }
        // 物品完全插入
        if (this.getParcels().isEmpty()) {
            // 删除NBT文件
            this.delete();
            return InsertResult.COMPLETE;
        }
        // 物品部分插入
        // 修改NBT文件
        this.save();
        return InsertResult.PART;
    }

    /**
     * 检查接收方是否有足够的权限执行接收物品的命令，这不会阻止物品发送，而是提示发送者
     */
    public void checkRecipientPermission() {
        PlayerList playerManager = this.server.getPlayerList();
        // 物品接收者玩家
        ServerPlayer recipientPlayer = playerManager.getPlayerByName(this.recipient);
        if (recipientPlayer == null) {
            return;
        }
        // 对方没有结束物品的权限，提示发送者
        ServerPlayer senderPlayer = playerManager.getPlayerByName(this.sender);
        checkRecipientPermission(senderPlayer, recipientPlayer);
    }

    public static void checkRecipientPermission(@Nullable ServerPlayer senderPlayer, ServerPlayer recipientPlayer) {
        // 检查接收者是否有接收物品的权限
        if (CommandUtils.canUseCommand(recipientPlayer.createCommandSourceStack(), CarpetOrgAdditionSettings.commandMail)) {
            return;
        }
        if (senderPlayer == null) {
            return;
        }
        // 将消息设置为灰色斜体
        Component message = TextBuilder.of(MailCommand.SEND.then("permission").translate()).setGrayItalic().build();
        MessageUtils.sendMessage(senderPlayer, message);
    }

    /**
     * 将快递信息保存到本地文件
     */
    public void save() throws IOException {
        File file = this.worldFormat.file(this.getId() + IOUtils.NBT_EXTENSION);
        CompoundTag nbt = this.writeNbt(this.server);
        NbtIo.write(nbt, file.toPath());
    }

    /**
     * 删除已经完成的快递
     */
    public void delete() {
        File file = this.worldFormat.file(this.getId() + IOUtils.NBT_EXTENSION);
        if (file.delete()) {
            return;
        }
        CarpetOrgAddition.LOGGER.warn("Failed to successfully delete file: {}", file);
    }

    /**
     * 完成寄件
     */
    public boolean isComplete() {
        return this.parcels.isEmpty();
    }

    /**
     * 将快递内容写入NBT
     */
    public CompoundTag writeNbt(MinecraftServer server) {
        NbtWriter writer = new NbtWriter(server, CURRENT_VERSION);
        writer.putString("sender", this.sender);
        writer.putString("recipient", this.recipient);
        if (this.uuid != null) {
            writer.putUuid("uuid", this.uuid);
        }
        writer.putBoolean("recall", this.recall);
        writer.putInventory("items", this.getParcels());
        writer.putLocalDateTime("time", this.time);
        return writer.toNbt();
    }

    /**
     * 从NBT读取快递信息
     */
    public static Parcel readNbt(MinecraftServer server, CompoundTag nbt, int id) {
        ParcelDataUpdater updater = new ParcelDataUpdater(server);
        int version = ParcelDataUpdater.getVersion(nbt);
        int vanillaVersion = ParcelDataUpdater.getVanillaVersion(nbt);
        NbtReader reader = new NbtReader(server, updater.update(nbt, version, vanillaVersion));
        String sender = reader.getStringOrElse("sender", UNKNOWN);
        String recipient = reader.getStringOrElse("recipient", UNKNOWN);
        UUID uuid = reader.getUuidNullable("uuid").orElse(null);
        boolean recall = reader.getBooleanOrElse("recall", false);
        ImmutableInventory inventory = reader.getInventory("items");
        LocalDateTime time = reader.getLocalDateTime("time");
        Parcel parcel = new Parcel(server, sender, recipient, uuid, inventory, id, time);
        parcel.recall = recall;
        return parcel;
    }

    /**
     * @return 快递单号
     */
    public int getId() {
        return this.id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public Component getTime() {
        return LocalizationKeys.Time.FORMAT.translate(
                this.time.getYear(),
                this.time.getMonthValue(),
                this.time.getDayOfMonth(),
                this.time.getHour(),
                this.time.getMinute(),
                this.time.getSecond()
        );
    }

    /**
     * @return 指定玩家是否是当前快递的发送者
     */
    public boolean isSender(ServerPlayer player) {
        return Objects.equals(this.sender, PlayerUtils.getName(player));
    }

    /**
     * @return 指定玩家是否是当前快递的接收者
     */
    public boolean isRecipient(ServerPlayer player) {
        return Objects.equals(this.recipient, PlayerUtils.getName(player));
    }

    /**
     * @return 快递是否已被撤回
     */
    public boolean isRecall() {
        return recall;
    }

    public int getCount() {
        int count = 0;
        for (ItemStack itemStack : this.getParcels()) {
            count += itemStack.getCount();
        }
        return count;
    }

    public Component getDisplayName() {
        Container inventory = this.getParcels();
        int size = inventory.getContainerSize();
        if (size == 0) {
            return TextBuilder.of(ServerUtils.getName(Items.AIR)).build();
        }
        ItemStack first = inventory.getItem(0);
        boolean exactlyTheSame = true;
        if (size > 1) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                // 比较物品和物品NBT
                if (InventoryUtils.canMerge(first, stack)) {
                    continue;
                }
                exactlyTheSame = false;
                // 只比较物品
                if (first.is(stack.getItem())) {
                    continue;
                }
                // 物品栏中有多种物品
                return LocalizationKeys.Item.ITEM.builder().setItalic().setHover(this.getItemList()).build();
            }
        }
        Component name = ServerUtils.getName(first.getItem());
        // 物品栏中所有物品类型都相同
        if (exactlyTheSame) {
            // 物品组件也完全相同
            return TextBuilder.of(name)
                    .setColor(first.getRarity())
                    .setHover(first)
                    .build();
        } else {
            return name;
        }
    }

    public Component getItemList() {
        Counter<Item> counter = new SimpleCounter<>();
        for (ItemStack parcel : this.getParcels()) {
            if (parcel.isEmpty()) {
                continue;
            }
            counter.add(parcel.getItem(), parcel.getCount());
        }
        TextJoiner joiner = new TextJoiner();
        for (Item item : counter) {
            joiner.newline(new TextJoiner().append(ServerUtils.getName(item)).append("*").append(counter.getCount(item)).join());
        }
        return joiner.join();
    }

    public Operation getPlayerOperation(ServerPlayer player) {
        if (this.isSender(player)) {
            return Operation.RECALL;
        }
        boolean ops = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        if (this.isRecipient(player)) {
            if (this.recall) {
                return ops ? Operation.INTERCEPT : Operation.VIEW;
            }
            return Operation.COLLECT;
        }
        if (ops) {
            return Operation.INTERCEPT;
        }
        return Operation.VIEW;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.isComplete()) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Parcel other = (Parcel) obj;
            if (other.isComplete()) {
                return false;
            }
            return this.id == other.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public int compareTo(@NotNull Parcel o) {
        return this.id - o.id;
    }

    private Container getParcels() {
        return InventoryUtils.removeEmpty(this.parcels);
    }

    /**
     * 向物品栏插入物品，返回插入结果
     */
    public enum InsertResult {
        /**
         * 物品完全插入物品栏
         */
        COMPLETE,
        /**
         * 物品部分插入物品栏
         */
        PART,
        /**
         * 物品没有插入物品栏
         */
        FAIL
    }

    /**
     * 玩家可以对快递进行什么操作
     */
    public enum Operation {
        /**
         * 接收物品
         */
        COLLECT,
        /**
         * 撤回物品
         */
        RECALL,
        /**
         * 拦截物品
         */
        INTERCEPT,
        /**
         * 查看物品
         */
        VIEW
    }
}
