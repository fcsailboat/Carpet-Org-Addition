package org.carpetorgaddition.util.express;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.wheel.WorldFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 快递
 */
public class Express implements Comparable<Express> {
    /**
     * 寄件人
     */
    private final String sender;
    /**
     * 收件人
     */
    private final String recipient;
    /**
     * 快递的内容
     */
    private final ItemStack express;
    /**
     * 快递是否已被撤回
     */
    private boolean cancel = false;
    /**
     * 快递单号
     */
    private final int id;
    MinecraftServer server;
    private final LocalDateTime time;
    private final WorldFormat worldFormat;
    public static final String EXPRESS = "express";

    public Express(MinecraftServer server, ServerPlayerEntity sender, ServerPlayerEntity recipient, int id) throws CommandSyntaxException {
        this.server = server;
        this.sender = sender.getName().getString();
        this.recipient = recipient.getName().getString();
        ItemStack mainHandStack = sender.getMainHandStack();
        if (mainHandStack.isEmpty()) {
            ItemStack offHandStack = sender.getOffHandStack();
            if (offHandStack.isEmpty()) {
                throw CommandUtils.createException("carpet.commands.mail.structure");
            }
            this.express = offHandStack.copyAndEmpty();
        } else {
            this.express = mainHandStack.copyAndEmpty();
        }
        this.id = id;
        this.time = LocalDateTime.now();
        this.worldFormat = new WorldFormat(server, EXPRESS);
    }

    public Express(MinecraftServer server, ServerPlayerEntity sender, ServerPlayerEntity recipient, ItemStack itemStack, int id) {
        this.server = server;
        this.sender = sender.getName().getString();
        this.recipient = recipient.getName().getString();
        this.express = itemStack;
        this.id = id;
        this.time = LocalDateTime.now();
        this.worldFormat = new WorldFormat(server, EXPRESS);
    }

    private Express(MinecraftServer server, String sender, String recipient, ItemStack express, int id, LocalDateTime time) {
        this.server = server;
        this.sender = sender;
        this.recipient = recipient;
        this.express = express;
        this.id = id;
        this.time = time;
        worldFormat = new WorldFormat(server, EXPRESS);
    }

    /**
     * 发送快递
     */
    public void sending() {
        PlayerManager playerManager = this.server.getPlayerManager();
        ServerPlayerEntity senderPlayer = playerManager.getPlayer(this.sender);
        ServerPlayerEntity recipientPlayer = playerManager.getPlayer(this.recipient);
        if (senderPlayer == null) {
            CarpetOrgAddition.LOGGER.error("快递由不存在的玩家发出");
            return;
        }
        if (recipientPlayer == null) {
            CarpetOrgAddition.LOGGER.error("向不存在的玩家发送快递");
            return;
        }
        // 向快递发送者发送发出快递的消息
        MutableText cancelText = TextConstants.clickRun("/mail cancel " + this.getId());
        Object[] senderArray = {recipientPlayer.getDisplayName(), this.express.getCount(), this.express.toHoverableText(), cancelText};
        MessageUtils.sendMessage(senderPlayer, TextUtils.translate("carpet.commands.mail.sending.sender", senderArray));
        // 向快递接受者发送发出快递的消息
        MutableText receiveText = TextConstants.clickRun("/mail receive " + this.getId());
        Object[] recipientArray = {senderPlayer.getDisplayName(), this.express.getCount(), this.express.toHoverableText(), receiveText};
        MessageUtils.sendMessage(recipientPlayer, TextUtils.translate("carpet.commands.mail.sending.recipient", recipientArray));
        // 在接收者位置播放音效
        playXpOrbPickupSound(recipientPlayer);
        CarpetOrgAddition.LOGGER.info("{}向{}发送了{}个{}", this.sender, this.recipient, this.express.getCount(), this.express.getName().getString());
    }

    /**
     * 接收快递
     */
    public void receive() throws IOException {
        PlayerManager playerManager = this.server.getPlayerManager();
        ServerPlayerEntity player = playerManager.getPlayer(this.recipient);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("接收快递的玩家不存在");
            return;
        }
        if (this.cancel) {
            // 快递已被撤回
            MessageUtils.sendMessage(player, TextUtils.translate("carpet.commands.mail.receive.cancel"));
            return;
        }
        int count = this.express.getCount();
        ItemStack copy = this.express.copy();
        switch (insertStack(player)) {
            case COMPLETE -> {
                // 物品完全接收
                MessageUtils.sendMessage(player,
                        TextUtils.translate("carpet.commands.mail.receive.success",
                                count, copy.toHoverableText()));
                // 通知发送者物品以接收
                Supplier<Text> message = () -> TextUtils.toGrayItalic(TextUtils.translate(
                        "carpet.commands.mail.sending.notice",
                        player.getDisplayName()));
                this.ifItExistsSendIt(this.sender, message);
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = copy.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player,
                        TextUtils.translate("carpet.commands.mail.receive.partial_reception",
                                count - surplusCount, surplusCount));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case FAIL -> MessageUtils.sendMessage(player,
                    TextUtils.translate("carpet.commands.mail.receive.insufficient_capacity"));
        }
    }

    /**
     * 撤回快递
     */
    public void cancel() throws IOException {
        PlayerManager playerManager = this.server.getPlayerManager();
        ServerPlayerEntity player = playerManager.getPlayer(this.sender);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("撤回快递的玩家不存在");
            return;
        }
        int count = this.express.getCount();
        ItemStack copy = this.express.copy();
        // 将快递内容放入物品栏
        switch (insertStack(player)) {
            case COMPLETE -> {
                MessageUtils.sendMessage(player, TextUtils.translate("carpet.commands.mail.cancel.success",
                        count, copy.toHoverableText()));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = copy.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player, TextUtils.translate("carpet.commands.mail.cancel.partial_reception",
                        count - surplusCount, surplusCount));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case FAIL ->
                // 物品未能成功放入物品栏
                    MessageUtils.sendMessage(player, TextUtils.translate("carpet.commands.mail.cancel.insufficient_capacity"));
            default -> throw new IllegalStateException();
        }
        // 如果接收者存在，向接收者发送物品被撤回的消息
        Supplier<Text> message = () -> TextUtils.toGrayItalic(TextUtils.translate("carpet.commands.mail.cancel.notice", player.getDisplayName()));
        this.ifItExistsSendIt(this.recipient, message);
        this.cancel = true;
    }

    /**
     * 播放物品拾取音效
     */
    public static void playItemPickupSound(ServerPlayerEntity player) {
        WorldUtils.playSound(player, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS);
    }

    /**
     * 播放经验球拾取音效
     */
    public static void playXpOrbPickupSound(ServerPlayerEntity recipientPlayer) {
        WorldUtils.playSound(recipientPlayer, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS);
    }

    /**
     * 接收每一件快递
     */
    public InsertResult receiveEach() throws IOException {
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.recipient);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("找不到接收快递的玩家，正在停止接收");
            throw new NullPointerException();
        }
        return this.insertStack(player);
    }

    /**
     * 撤回每一件快递
     */
    public InsertResult cancelEach() throws IOException {
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.sender);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("找不到撤回快递的玩家，正在停止撤回");
            throw new NullPointerException();
        }
        return this.insertStack(player);
    }

    /**
     * 向物品栏里插入物品
     */
    private InsertResult insertStack(ServerPlayerEntity player) throws IOException {
        int count = this.express.getCount();
        player.getInventory().insertStack(this.express);
        // 物品没有插入
        if (count == this.express.getCount()) {
            return InsertResult.FAIL;
        }
        // 物品完全插入
        if (this.express.isEmpty()) {
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
     * 如果指定玩家存在，则向该玩家发送消息
     *
     * @param playerName 要查找的玩家名称
     * @param message    要发送的消息，使用Supplier包装，只在玩家存在时获取消息
     */
    private void ifItExistsSendIt(String playerName, Supplier<Text> message) {
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(playerName);
        if (player == null) {
            return;
        }
        MessageUtils.sendMessage(player.getCommandSource(), message.get());
    }

    /**
     * 检查接收方是否有足够的权限执行接收物品的命令，这不会阻止物品发送，而是提示发送者
     */
    public void checkRecipientPermission() {
        PlayerManager playerManager = this.server.getPlayerManager();
        // 物品接收者玩家
        ServerPlayerEntity recipientPlayer = playerManager.getPlayer(this.recipient);
        if (recipientPlayer == null) {
            return;
        }
        // 对方没有结束物品的权限，提示发送者
        ServerPlayerEntity senderPlayer = playerManager.getPlayer(this.sender);
        checkRecipientPermission(senderPlayer, recipientPlayer);
    }

    public static void checkRecipientPermission(@Nullable ServerPlayerEntity senderPlayer, ServerPlayerEntity recipientPlayer) {
        // 检查接收者是否有接收物品的权限
        if (CommandHelper.canUseCommand(recipientPlayer.getCommandSource(), CarpetOrgAdditionSettings.commandMail)) {
            return;
        }
        if (senderPlayer == null) {
            return;
        }
        // 将消息设置为灰色斜体
        MutableText message = TextUtils.toGrayItalic(TextUtils.translate("carpet.commands.mail.sending.permission"));
        MessageUtils.sendMessage(senderPlayer.getCommandSource(), message);
    }

    /**
     * 将快递信息保存到本地文件
     */
    public void save() throws IOException {
        NbtIo.write(this.writeNbt(this.server), this.worldFormat.file(this.getId() + ".nbt").toPath());
    }

    /**
     * 删除已经完成的快递
     */
    public void delete() {
        File file = this.worldFormat.getFile(this.getId() + ".nbt");
        if (file.delete()) {
            return;
        }
        CarpetOrgAddition.LOGGER.warn("未能成功删除名为{}的文件", file);
    }

    /**
     * 完成寄件
     */
    public boolean complete() {
        return this.express.isEmpty();
    }

    /**
     * 将快递内容写入NBT
     */
    public NbtCompound writeNbt(MinecraftServer server) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("sender", this.sender);
        nbt.putString("recipient", this.recipient);
        nbt.putBoolean("cancel", this.cancel);
        nbt.putInt("id", this.id);
        int[] args = {time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour(), time.getMinute(), time.getSecond()};
        nbt.putIntArray("time", args);
        nbt.put("item", this.express.encode(server.getRegistryManager(), new NbtCompound()));
        return nbt;
    }

    /**
     * 从NBT读取快递信息
     */
    public static Express readNbt(MinecraftServer server, NbtCompound nbt) {
        String sender = nbt.getString("sender");
        String recipient = nbt.getString("recipient");
        boolean cancel = nbt.getBoolean("cancel");
        ItemStack stack = ItemStack.fromNbt(server.getRegistryManager(), nbt.getCompound("item")).orElse(ItemStack.EMPTY);
        int id = nbt.getInt("id");
        int[] times = nbt.getIntArray("time");
        LocalDateTime localDateTime = LocalDateTime.of(times[0], times[1], times[2], times[3], times[4], times[5]);
        Express express = new Express(server, sender, recipient, stack, id, localDateTime);
        express.cancel = cancel;
        return express;
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

    public MutableText getTime() {
        return TextUtils.translate("carpet.command.time.format",
                this.time.getYear(),
                this.time.getMonthValue(),
                this.time.getDayOfMonth(),
                this.time.getHour(),
                this.time.getMinute(),
                this.time.getSecond());
    }

    public ItemStack getExpress() {
        return express;
    }

    /**
     * @return 指定玩家是否是当前快递的发送者
     */
    public boolean isSender(ServerPlayerEntity player) {
        return Objects.equals(this.sender, player.getName().getString());
    }

    /**
     * @return 指定玩家是否是当前快递的接收者
     */
    public boolean isRecipient(ServerPlayerEntity player) {
        return Objects.equals(this.recipient, player.getName().getString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this.complete()) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Express other = (Express) obj;
            if (other.complete()) {
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
    public int compareTo(@NotNull Express o) {
        return this.id - o.id;
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
}
