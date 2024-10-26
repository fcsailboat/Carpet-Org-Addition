package org.carpetorgaddition.util.task.playerscheduletask;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.fakeplayer.FakePlayerSerial;
import org.jetbrains.annotations.NotNull;

public class DelayedLoginTask extends PlayerScheduleTask {
    private final MinecraftServer server;
    FakePlayerSerial serial;
    private long delayed;

    public DelayedLoginTask(MinecraftServer server, FakePlayerSerial serial, long delayed) {
        this.server = server;
        this.serial = serial;
        this.delayed = delayed;
    }

    @Override
    public void tick() {
        if (this.delayed == 0L) {
            try {
                // 生成假玩家
                serial.spawn(this.server);
            } catch (CommandSyntaxException e) {
                CarpetOrgAddition.LOGGER.error("玩家{}已存在", this.serial.getFakePlayerName(), e);
            } catch (RuntimeException e) {
                CarpetOrgAddition.LOGGER.error("玩家{}未能在指定时间上线", this.serial.getFakePlayerName(), e);
            } finally {
                // 将此任务设为已执行结束
                this.delayed = -1L;
            }
        } else {
            this.delayed--;
        }
    }

    @Override
    public String getPlayerName() {
        return serial.getFakePlayerName();
    }

    @Override
    public void onCancel(CommandContext<ServerCommandSource> context) {
        MutableText time = getDisplayTime();
        MutableText displayName = this.serial.getDisplayName().copy();
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.login.cancel", displayName, time);
    }

    // 获取带有悬停提示的时间
    private @NotNull MutableText getDisplayTime() {
        return TextUtils.hoverText(TextConstants.tickToTime(this.delayed), TextConstants.tickToRealTime(this.delayed));
    }

    @Override
    public void sendEachMessage(ServerCommandSource source) {
        MessageUtils.sendMessage(source, "carpet.commands.playerManager.schedule.login",
                this.serial.getDisplayName(), this.getDisplayTime());
    }

    public void setDelayed(long delayed) {
        this.delayed = delayed;
    }

    public Text getInfo() {
        return this.serial.info();
    }

    @Override
    public boolean stopped() {
        return this.delayed < 0L;
    }

    @Override
    public String getLogName() {
        return this.serial.getFakePlayerName() + "延迟上线";
    }
}
