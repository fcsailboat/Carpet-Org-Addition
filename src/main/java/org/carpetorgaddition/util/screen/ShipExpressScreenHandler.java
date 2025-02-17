package org.carpetorgaddition.util.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.ServerPeriodicTaskManager;
import org.carpetorgaddition.periodic.express.Express;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.inventory.AutoGrowInventory;
import org.carpetorgaddition.util.inventory.ImmutableInventory;

import java.io.IOException;

public class ShipExpressScreenHandler extends GenericContainerScreenHandler {
    private final Inventory inventory;
    private final ExpressManager expressManager;
    private final MinecraftServer server;
    private final ServerPlayerEntity sourcePlayer;
    private final ServerPlayerEntity targetPlayer;

    public ShipExpressScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ServerPlayerEntity sourcePlayer,
            ServerPlayerEntity targetPlayer,
            Inventory inventory
    ) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.server = targetPlayer.server;
        this.expressManager = ServerPeriodicTaskManager.getManager(this.server).getExpressManager();
        this.sourcePlayer = sourcePlayer;
        this.targetPlayer = targetPlayer;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (this.inventory.isEmpty()) {
            return;
        }
        // 快递接收者可能在发送者发送快递时退出游戏
        if (this.targetPlayer.isRemoved()) {
            MessageUtils.sendErrorMessage(this.sourcePlayer.getCommandSource(), "carpet.commands.multiple.no_player");
            // 将GUI中的物品放回玩家物品栏
            this.dropInventory(this.sourcePlayer, this.inventory);
            return;
        }
        AutoGrowInventory autoGrowInventory = new AutoGrowInventory();
        // 合并可堆叠的物品
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack itemStack = this.inventory.getStack(i);
            // 不要发送空气物品
            if (itemStack.isEmpty()) {
                continue;
            }
            autoGrowInventory.addStack(itemStack);
        }
        // 发送物品
        for (int i = 0; i < autoGrowInventory.size(); i++) {
            ItemStack stack = autoGrowInventory.getStack(i);
            // 前面已经对物品进行了整理，所以遇到空物品时，说明物品已发送完毕
            if (stack.isEmpty()) {
                break;
            }
            Express express = new Express(this.server, this.sourcePlayer, this.targetPlayer, stack, this.expressManager.generateNumber());
            try {
                expressManager.putNoMessage(express);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("批量发送物品时遇到意外错误", e);
                MessageUtils.sendErrorMessage(this.sourcePlayer.getCommandSource(), e, "carpet.commands.multiple.error");
                return;
            }
        }
        sendFeedback(autoGrowInventory);
    }

    // 发送命令反馈
    public void sendFeedback(AutoGrowInventory inventory) {
        int count = 0;
        ItemStack firstStack = inventory.getStack(0);
        // 定义变量记录查找状态
        // 如果为0，表示物品栏里只有一种物品，并且NBT也相同
        // 如果为1，表示物品栏里只有一种物品，但是NBT不相同
        // 如果为2，表示物品栏里有多种物品，不考虑NBT
        int onlyOneKind = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            count += stack.getCount();
            // 比较物品和物品NBT
            if (onlyOneKind != 0 && ItemStack.areEqual(firstStack, stack)) {
                continue;
            }
            onlyOneKind = 1;
            // 只比较物品
            if (firstStack.isOf(stack.getItem())) {
                continue;
            }
            onlyOneKind = 2;
            break;
        }
        Text playerName = this.targetPlayer.getDisplayName();
        MutableText command = TextConstants.clickRun("/mail cancel");
        Object[] args = switch (onlyOneKind) {
            case 0 -> {
                MutableText itemCount = TextConstants.itemCount(count, firstStack.getMaxCount());
                yield new Object[]{playerName, itemCount, firstStack.toHoverableText(), command};
            }
            case 1 -> {
                // 物品名称
                Text hoverableText = firstStack.getItem().getDefaultStack().toHoverableText();
                // 物品堆叠数
                MutableText itemCount = TextConstants.itemCount(count, firstStack.getMaxCount());
                yield new Object[]{playerName, itemCount, hoverableText, command};
            }
            case 2 -> {
                // 不显示物品堆叠组数，但鼠标悬停可以显示物品栏
                MutableText itemText = TextUtils.translate("carpet.command.item.item");
                yield new Object[]{playerName, count, TextConstants.inventory(itemText, inventory), command};
            }
            default -> throw new IllegalStateException();
        };
        // 向物品发送者发送消息
        MessageUtils.sendMessage(this.sourcePlayer, "carpet.commands.mail.sending.multiple", args);
        // 向物品接收者发送消息
        MessageUtils.sendMessage(this.targetPlayer, "carpet.commands.mail.receive.multiple",
                this.sourcePlayer.getDisplayName(), args[1], args[2], TextConstants.clickRun("/mail receive"));
        Express.playXpOrbPickupSound(this.targetPlayer);
        Express.checkRecipientPermission(this.sourcePlayer, this.targetPlayer);
        // 日志输出
        if (onlyOneKind == 2) {
            CarpetOrgAddition.LOGGER.info("{}向{}发送了{}",
                    this.sourcePlayer.getName().getString(),
                    this.targetPlayer.getName().getString(),
                    new ImmutableInventory(inventory));
        } else {
            CarpetOrgAddition.LOGGER.info("{}向{}发送了{}个{}",
                    this.sourcePlayer.getName().getString(),
                    this.targetPlayer.getName().getString(),
                    ((MutableText) args[1]).getString(),
                    firstStack.getName().getString());
        }
    }
}
